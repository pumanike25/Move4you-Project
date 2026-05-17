package com.example.maps4u;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;

public class FirebaseHelper {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private static final String TAG = "FirebaseHelper";

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    // --- USER ---
    public void saveUser(String userId, User user, FirestoreCallback<Void> callback) {
        db.collection("users").document(userId)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getUser(String userId, FirestoreCallback<User> callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onCallback(user);
                    } else {
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // --- CAR DATA  ---
    public void saveCarData(String userId, String carDataJson) {
        db.collection("users").document(userId)
                .update("carData", carDataJson)
                .addOnFailureListener(e -> Log.e(TAG, "Error saving car data", e));
    }

    // --- CUSTOM CAR PROFILES  ---
    public void saveCustomCarProfile(String userId, Car car, FirestoreCallback<Void> callback) {
        db.collection("users").document(userId)
                .collection("car_profiles")
                .add(car)
                .addOnSuccessListener(docRef -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getCustomCarProfiles(String userId, FirestoreCallback<List<Car>> callback) {
        db.collection("users").document(userId)
                .collection("car_profiles")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Car> cars = querySnapshot.toObjects(Car.class);
                    callback.onCallback(cars);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveFavoriteLocation(String userId, String customName, String address, double lat, double lng, FirestoreCallback<Void> callback) {
        Map<String, Object> favorite = new HashMap<>();
        favorite.put("customName", customName);
        favorite.put("address", address);
        favorite.put("latitude", lat);
        favorite.put("longitude", lng);
        favorite.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(userId)
                .collection("favorites")
                .add(favorite)
                .addOnSuccessListener(documentReference -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void checkFavoriteExists(String userId, String address, FirestoreCallback<String> callback) {
        db.collection("users").document(userId).collection("favorites")
                .whereEqualTo("address", address)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Returnăm ID-ul documentului pentru a putea fi șters
                        callback.onCallback(queryDocumentSnapshots.getDocuments().get(0).getId());
                    } else {
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> callback.onCallback(null));
    }

    public void getFavoriteLocations(String currentUserId, FirestoreCallback<List<FavoriteLocation>> callback) {
        db.collection("users").document(currentUserId).collection("favorites").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FavoriteLocation> favs = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String address = doc.getString("address");
                        String customName = doc.getString("customName");
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");

                        if (address != null && lat != null && lng != null) {
                            if (customName == null || customName.isEmpty()) {
                                customName = address;
                            }
                            favs.add(new FavoriteLocation(doc.getId(), customName, address, lat, lng));
                        }
                    }
                    callback.onCallback(favs);
                }).addOnFailureListener(callback::onFailure);
    }

    public void updateFavoriteName(String userId, String locationId, String newName, FirestoreCallback<Void> callback) {
        db.collection("users").document(userId).collection("favorites").document(locationId)
                .update("customName", newName)
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteFavoriteLocation(String userId, String locationId, FirestoreCallback<Void> callback) {
        db.collection("users").document(userId).collection("favorites").document(locationId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }





    // --- HISTORY ---
    public void addRouteToHistory(String userId, String origin, String destination, String mode) {
        RouteHistory route = new RouteHistory(origin, destination, mode);
        db.collection("users").document(userId)
                .collection("history")
                .add(route);
    }

    public void getUserHistory(String userId, FirestoreCallback<QuerySnapshot> callback) {
        db.collection("users").document(userId)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(callback::onCallback)
                .addOnFailureListener(callback::onFailure);
    }

    // COMMENTS
    public void addPlaceComment(String placeId, String userId, String userName, String userImageUrl, String text, FirestoreCallback<Void> callback) {
        PlaceComment comment = new PlaceComment();
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setUserImageUrl(userImageUrl);
        comment.setText(text);
        comment.setTimestamp(System.currentTimeMillis());

        db.collection("places_data").document(placeId).collection("comments").add(comment)
                .addOnSuccessListener(docRef -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getPlaceComments(String placeId, FirestoreCallback<List<PlaceComment>> callback) {
        db.collection("places_data").document(placeId).collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PlaceComment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        PlaceComment comment = doc.toObject(PlaceComment.class);
                        if (comment != null) {
                            comment.setId(doc.getId());
                            comments.add(comment);
                        }
                    }
                    callback.onCallback(comments);
                }).addOnFailureListener(callback::onFailure);
    }

    public void setCommentVote(String placeId, String commentId, String userId, boolean isHelpful, FirestoreCallback<Void> callback) {
        DocumentReference commentRef = db.collection("places_data").document(placeId).collection("comments").document(commentId);

        if (isHelpful) {
            commentRef.update(
                    "helpful", FieldValue.arrayUnion(userId),
                    "unhelpful", FieldValue.arrayRemove(userId)
            ).addOnSuccessListener(aVoid -> callback.onCallback(null)).addOnFailureListener(callback::onFailure);
        } else {
            commentRef.update(
                    "unhelpful", FieldValue.arrayUnion(userId),
                    "helpful", FieldValue.arrayRemove(userId)
            ).addOnSuccessListener(aVoid -> callback.onCallback(null)).addOnFailureListener(callback::onFailure);
        }
    }

    public void addReplyToComment(String placeId, String commentId, String userId, String userName, String userImageUrl, String text, FirestoreCallback<Void> callback) {
        DocumentReference commentRef = db.collection("places_data").document(placeId).collection("comments").document(commentId);

        CommentReply reply = new CommentReply(userId, userName, userImageUrl, text, System.currentTimeMillis());

        commentRef.update("replies", FieldValue.arrayUnion(reply))
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }


    // Chat stuff

    // creating a unique conversation ID
    public String getChatRoomId(String uid1, String uid2) {
        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    public void sendMessage(String senderId, String receiverId, String text, String type) {
        String roomId = getChatRoomId(senderId, receiverId);
        ChatMessage message = new ChatMessage(senderId, receiverId, text, System.currentTimeMillis(), type);

        db.collection("chats").document(roomId)
                .collection("messages")
                .add(message)
                .addOnFailureListener(e -> Log.e(TAG, "Eroare la trimiterea mesajului", e));
    }

    // accounting the msg in real time4 using ListenerRegistration for stopping it when the user leaves
    public com.google.firebase.firestore.ListenerRegistration listenForMessages(String currentUserId, String targetUserId, FirestoreCallback<List<ChatMessage>> callback) {
        String roomId = getChatRoomId(currentUserId, targetUserId);

        return db.collection("chats").document(roomId).collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }
                    if (value != null) {
                        List<ChatMessage> messages = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            ChatMessage msg = doc.toObject(ChatMessage.class);
                            if (msg != null) {
                                msg.setId(doc.getId());

                                messages.add(msg);
                            }
                        }
                        callback.onCallback(messages);
                    }
                });
    }

    public void sendMeetupRequest(String senderId, String receiverId, String name, String description, String address, double lat, double lng, long scheduledTime) {
        String roomId = getChatRoomId(senderId, receiverId);

        ChatMessage meetupMsg = new ChatMessage(senderId, receiverId, "Meetup Invitation", System.currentTimeMillis(), "meetup");
        meetupMsg.setMeetupName(name);
        meetupMsg.setMeetupDescription(description);
        meetupMsg.setMeetupAddress(address);
        meetupMsg.setMeetupLat(lat);
        meetupMsg.setMeetupLng(lng);
        meetupMsg.setMeetupTime(scheduledTime);
        meetupMsg.setMeetupStatus("PENDING");

        db.collection("chats").document(roomId).collection("messages").add(meetupMsg);
    }

    public void updateMeetupStatus(String roomId, String messageId, String newStatus, FirestoreCallback<Void> callback) {
        db.collection("chats").document(roomId).collection("messages").document(messageId)
                .update("meetupStatus", newStatus)
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void saveAcceptedMeetup(String userId, ChatMessage meetup) {
        db.collection("users").document(userId).collection("accepted_meetups")
                .document(meetup.getId()) // Folosim același ID ca al mesajului pentru a evita duplicatele
                .set(meetup);
    }

    public void getAcceptedMeetups(String userId, FirestoreCallback<List<ChatMessage>> callback) {
        db.collection("users").document(userId).collection("accepted_meetups")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatMessage> meetups = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatMessage meetup = doc.toObject(ChatMessage.class);
                        if (meetup != null) {
                            meetups.add(meetup);
                        }
                    }
                    callback.onCallback(meetups);
                })
                .addOnFailureListener(callback::onFailure);
    }


    public void savePublicKey(String publicKeyBase64) {
        String currentUserId = getCurrentUserId();

        if (currentUserId != null && publicKeyBase64 != null) {
            db.collection("users").document(currentUserId)
                    .update("publicKey", publicKeyBase64)
                    .addOnSuccessListener(aVoid -> {
                        System.out.println("Public key saved successfully for user: " + currentUserId);
                    })
                    .addOnFailureListener(e -> {
                        System.err.println("Error saving public key: " + e.getMessage());
                        e.printStackTrace();
                    });
        }
    }

    public void sendEncryptedMessage(String senderId, String receiverId, String textForSender, String textForReceiver, String type) {
        String roomId = getChatRoomId(senderId, receiverId);
        String messageId = db.collection("chats").document(roomId).collection("messages").document().getId();

        ChatMessage message = new ChatMessage(senderId, receiverId, "", System.currentTimeMillis(), type);
        message.setId(messageId);

        // Set new flags and crypted texts
        message.setEncrypted(true);
        message.setTextForSender(textForSender);
        message.setTextForReceiver(textForReceiver);

        db.collection("chats").document(roomId).collection("messages").document(messageId).set(message);
    }

    // --- BIOMETRICS ---
    public void saveBiometricData(String userId, BiometricData data) {
        db.collection("users").document(userId)
                .collection("biometrics").document("current")
                .set(data);
    }

    public void getBiometricData(String userId, FirestoreCallback<BiometricData> callback) {
        db.collection("users").document(userId)
                .collection("biometrics").document("current")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onCallback(doc.toObject(BiometricData.class));
                    } else {
                        callback.onCallback(new BiometricData(0, 0, 0, "", 0));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // --- DAILY STEPS ---
    public void saveDailySteps(String userId, String date, int steps) {
        Map<String, Object> data = new HashMap<>();
        data.put("steps", steps);
        data.put("date", date);

        db.collection("users").document(userId)
                .collection("daily_steps").document(date)
                .set(data, SetOptions.merge());
    }

    public void getDailySteps(String userId, String date, FirestoreCallback<Integer> callback) {
        db.collection("users").document(userId)
                .collection("daily_steps").document(date)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("steps")) {
                        Long steps = doc.getLong("steps");
                        callback.onCallback(steps != null ? steps.intValue() : 0);
                    } else {
                        callback.onCallback(0);
                    }
                })
                .addOnFailureListener(e -> callback.onCallback(0));
    }

    // SOCIAL PART
    public void searchUsers(String queryText, FirestoreCallback<List<User>> callback) {
        if (queryText == null || queryText.isEmpty()) {
            callback.onCallback(new ArrayList<>());
            return;
        }

        db.collection("users")
                .orderBy("username")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> usersList = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId()); // Salvam ID-ul documentului ca UID
                            usersList.add(user);
                        }
                    }
                    callback.onCallback(usersList);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // send friend req
    public void sendFriendRequest(String currentUserId, String targetUserId, FirestoreCallback<Void> callback) {
        Map<String, Object> request = new HashMap<>();
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(targetUserId)
                .collection("friend_requests").document(currentUserId)
                .set(request)
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }

    // friend req status
    public void checkFriendshipStatus(String currentUserId, String targetUserId, FirestoreCallback<String> callback) {
        db.collection("users").document(currentUserId)
                .collection("friends").document(targetUserId)
                .get()
                .addOnSuccessListener(friendDoc -> {
                    if (friendDoc.exists()) {
                        callback.onCallback("friends");
                        return;
                    }

                    db.collection("users").document(targetUserId)
                            .collection("friend_requests").document(currentUserId)
                            .get()
                            .addOnSuccessListener(requestDoc -> {
                                if (requestDoc.exists()) {
                                    callback.onCallback("request_sent");
                                } else {
                                    callback.onCallback("none");
                                }
                            })
                            .addOnFailureListener(e -> callback.onCallback("none"));
                })
                .addOnFailureListener(e -> callback.onCallback("none"));
    }
    public void acceptFriendRequest(String currentUserId, String requesterId, FirestoreCallback<Void> callback) {
        Map<String, Object> friendData = new HashMap<>();
        friendData.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(currentUserId).collection("friends").document(requesterId).set(friendData)
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(requesterId).collection("friends").document(currentUserId).set(friendData)
                            .addOnSuccessListener(aVoid2 -> {
                                db.collection("users").document(currentUserId).collection("friend_requests").document(requesterId).delete()
                                        .addOnSuccessListener(aVoid3 -> callback.onCallback(null));
                            });
                }).addOnFailureListener(callback::onFailure);
    }

    // unfriend
    public void removeFriendOrRequest(String currentUserId, String targetUserId, FirestoreCallback<Void> callback) {
        // Stergem din friends (daca existau)
        db.collection("users").document(currentUserId).collection("friends").document(targetUserId).delete();
        db.collection("users").document(targetUserId).collection("friends").document(currentUserId).delete();

        // Stergem din requests (daca era o cerere in asteptare)
        db.collection("users").document(targetUserId).collection("friend_requests").document(currentUserId).delete();
        db.collection("users").document(currentUserId).collection("friend_requests").document(targetUserId).delete()
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(callback::onFailure);
    }
    public void getAllDailySteps(String userId, FirestoreCallback<List<Map<String, Object>>> callback) {
        db.collection("users").document(userId)
                .collection("daily_steps")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> stepsList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        stepsList.add(doc.getData());
                    }
                    callback.onCallback(stepsList);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface FirestoreCallback<T> {
        void onCallback(T data);
        default void onFailure(Exception e) {
            Log.e(TAG, "Firebase Operation Failed: " + e.getMessage());
        }
    }

    public void getFriendsList(String currentUserId, FirestoreCallback<List<User>> callback) {
        db.collection("users").document(currentUserId).collection("friends").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> friendIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        friendIds.add(doc.getId()); // Luam UID-urile prietenilor
                    }
                    fetchUsersByIds(friendIds, callback);
                }).addOnFailureListener(callback::onFailure);
    }

    public void getFriendRequests(String currentUserId, FirestoreCallback<List<User>> callback) {
        db.collection("users").document(currentUserId).collection("friend_requests").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> requesterIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        requesterIds.add(doc.getId());
                    }
                    fetchUsersByIds(requesterIds, callback);
                }).addOnFailureListener(callback::onFailure);
    }

    public void getUnlockedTrophies(String currentUserId, FirestoreCallback<Map<String, Long>> callback) {
        db.collection("users").document(currentUserId).collection("unlocked_trophies").get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Long> trophies = new HashMap<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long timestamp = doc.getLong("timestamp");
                        if (timestamp != null) {
                            trophies.put(doc.getId(), timestamp);
                        }
                    }
                    callback.onCallback(trophies);
                }).addOnFailureListener(e -> callback.onCallback(new HashMap<>())); // returning an empty list in case of error
    }

    public void saveUnlockedTrophy(String currentUserId, String trophyId, FirestoreCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(currentUserId).collection("unlocked_trophies").document(trophyId)
                .set(data)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onCallback(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    private void fetchUsersByIds(List<String> uids, FirestoreCallback<List<User>> callback) {
        if (uids.isEmpty()) {
            callback.onCallback(new ArrayList<>());
            return;
        }

        List<User> usersList = new ArrayList<>();
        int[] completedQueries = {0}; // var for knowing when all the data is fetched

        for (String uid : uids) {
            getUser(uid, new FirestoreCallback<User>() {
                @Override
                public void onCallback(User user) {
                    if (user != null) {
                        user.setUid(uid);
                        usersList.add(user);
                    }
                    checkIfDone();
                }

                @Override
                public void onFailure(Exception e) {
                    checkIfDone();
                }

                private void checkIfDone() {
                    completedQueries[0]++;
                    if (completedQueries[0] == uids.size()) {
                        callback.onCallback(usersList);
                    }
                }
            });
        }
    }

}