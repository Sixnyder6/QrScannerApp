/* eslint-disable */
"use strict";

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// Триггер: новый документ в fcm_queue → отправляем FCM
exports.sendPushNotification = functions.firestore
  .document("fcm_queue/{docId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    if (data.processed) return null;

    try {
      const userDoc = await db.collection("internal_users").doc(data.recipientUserId).get();
      if (!userDoc.exists) return markProcessed(snap.ref, "user_not_found");

      const fcmToken = userDoc.data().fcmToken;
      if (!fcmToken) return markProcessed(snap.ref, "no_token");

      const message = {
        token: fcmToken,
        data: {
          type: data.type || "system",
          title: data.title || "QrScannerApp",
          body: data.body || "",
          roomId: data.roomId || "",
          isMention: String(data.isMention || false),
        },
        notification: {
          title: data.title || "QrScannerApp",
          body: data.body || "",
        },
        android: {
          priority: data.isMention ? "high" : "normal",
          notification: {
            channelId: getChannelId(data.type, data.isMention),
            sound: "default",
          },
        },
      };

      const response = await messaging.send(message);
      console.log("FCM sent:", response);
      return markProcessed(snap.ref, "sent");
    } catch (error) {
      console.error("FCM send error:", error);
      return markProcessed(snap.ref, `error: ${error.message}`);
    }
  });

// Триггер: новое сообщение в чате → уведомляем участников комнаты
exports.onChatMessage = functions.firestore
  .document("chats/{roomId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const roomId = context.params.roomId;

    if (message.type === "SYSTEM" || message.type === "ALERT") return null;
    if (!message.senderId || message.senderId === "system") return null;

    try {
      const usersSnap = await db.collection("internal_users").where("fcmToken", "!=", null).get();
      const batch = [];

      usersSnap.docs.forEach((userDoc) => {
        const userId = userDoc.id;
        const userData = userDoc.data();
        const fcmToken = userData.fcmToken;

        if (userId === message.senderId) return;
        if (!fcmToken) return;
        if (!hasRoomAccess(userData.role, roomId)) return;

        const mentionedNames = message.mentionedNames || [];
        const firstName = (userData.displayName || "").split(" ")[0].toLowerCase();
        const isMention = mentionedNames.some((name) =>
          name.toLowerCase().includes(firstName)
        );

        const roomName = getRoomName(roomId);
        const title = isMention
          ? `📣 ${message.senderName} упомянул вас`
          : `💬 ${message.senderName} в ${roomName}`;
        const body = message.text.substring(0, 100);

        batch.push(
          db.collection("fcm_queue").add({
            recipientUserId: userId,
            type: "chat",
            title: title,
            body: body,
            roomId: roomId,
            isMention: isMention,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            processed: false,
          })
        );
      });

      await Promise.all(batch);
      console.log(`Queued ${batch.length} notifications for room ${roomId}`);
      return null;
    } catch (error) {
      console.error("onChatMessage error:", error);
      return null;
    }
  });

// Очистка старых обработанных уведомлений раз в день
exports.cleanupFcmQueue = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async () => {
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - 7);

    const oldDocs = await db
      .collection("fcm_queue")
      .where("processed", "==", true)
      .where("processedAt", "<", cutoff)
      .get();

    const batch = db.batch();
    oldDocs.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();

    console.log(`Cleaned ${oldDocs.size} old fcm_queue docs`);
    return null;
  });

function getChannelId(type, isMention) {
  if (type === "chat" && isMention) return "channel_chat_mention";
  if (type === "chat") return "channel_chat";
  if (type === "shift_request" || type === "shift_response") return "channel_shifts";
  if (type === "task") return "channel_tasks";
  return "channel_system";
}

function markProcessed(ref, status) {
  return ref.update({
    processed: true,
    processedAt: admin.firestore.FieldValue.serverTimestamp(),
    status: status,
  });
}

function hasRoomAccess(role, roomId) {
  const access = {
    admin: ["general", "muvers", "managers", "shifts", "alerts"],
    inventory_manager: ["general", "managers", "shifts", "alerts"],
    muver: ["general", "muvers", "shifts"],
    electrician: ["general", "shifts"],
    technic: ["general", "shifts"],
  };
  return (access[role] || ["general"]).includes(roomId);
}

function getRoomName(roomId) {
  const names = {
    general: "Общий",
    muvers: "Мувёры",
    managers: "Склад",
    shifts: "Смена",
    alerts: "Оповещения",
  };
  return names[roomId] || roomId;
}