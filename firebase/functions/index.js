const functions = require("firebase-functions");
const admin = require("firebase-admin");
const stripe = require("stripe")(
  "sk_test_51JCRzOSADReF0qa1JJ3wqe5yZJf96qXuGzLui0jRpbL9jSAdFll2a1jEoE8w1dWDW0qAVcMnHB6PFK2KWULHiLmZ00IYGkUU7S"
);
const Razorpay = require("razorpay");
const crypto = require("crypto");
const axios = require('axios').default;

admin.initializeApp();

const phoneNumbers = ["+911231231234", "+915777777777", "+914444488888", "+916767676767"]

const secret_key = "TgPK6mRPfIxm5wWKpnRmQtdM";
const db = admin.firestore();

const razorpayKeyId = "rzp_test_DqWYBFbaG5xwII";

const razorpay = new Razorpay({
  key_id: razorpayKeyId,
  key_secret: secret_key,
});

const USER_ID = "userId";
const NAME = "name";
const PHONE_NO = "phoneNo";
const EMAIL = "email";
const LAST_PLACE = "lastPlace";
const BALANCE = "balance";
const PHOTO_URL = "photoUrl";
const AMOUNT = "amount";
const CUSTOMER = "customer";
const CURRENCY = "currency";
const PAYMENT_METHOD = "payment_method";
const REQUIRES_CONFIRMATION = "requires_confirmation";
const BASE_STRIPE_CUSTOMERS_PATH = "stripe_customers";
const PAYMENTS_PATH = "payments";
const PAYMENT_METHODS = "payment_methods";
const RECEIPT_ID = "receiptId";

exports.createRazorpayOrder = functions.firestore
	.document("users/{userId}/orders/{orderId}")
	.onCreate(async (snap, context) => {
		
		if (snap === null) {
			return null;
		}

		const amount = snap.get("prices.total");
		const currency = snap.get(CURRENCY);
		const receipt = snap.get(RECEIPT_ID);

		const options = {
			amount: amount, // amount in the smallest currency unit
			currency: currency,
			receipt: receipt,
		};

		const order = await razorpay.orders.create(options);
		const orderId = order.id;

		const batch = db.batch();

		const razorpayOrderRef = db
			.collection("users")
			.doc(context.params.userId)
			.collection("razorpay_orders")
			.doc(orderId);

		batch.set(razorpayOrderRef, order);

		const orderRef = db
			.collection("users")
			.doc(context.params.userId)
			.collection("orders")
			.doc(context.params.orderId);

		batch.update(orderRef, { razorpayOrderId: orderId });

		return await batch.commit();
	});

exports.confirmRazorpayPayment = functions.firestore
  	.document("users/{userId}/payments/{paymentId}")
  	.onCreate(async (snap, context) => {
    	const orderId = snap.get("orderId");
      const razorpayOrderId = snap.get("razorpayOrderId")
    	const paymentId = snap.get("paymentId");
    	const signature = snap.get("paymentSignature");
		const amount = snap.get("amount");


    	const generated_signature = crypto
      		.createHmac("sha256", secret_key)
      		.update(`${razorpayOrderId}|${paymentId}`)
      		.digest("hex");

    	const batch = db.batch();

    	if (generated_signature == signature) {
      		const paymentsRef = db
        		.collection("users")
        		.doc(context.params.userId)
        		.collection("payments")
        		.doc(paymentId);

      		batch.update(paymentsRef, { status: "successful" });

			const ordersRef = db
				.collection("users")
				.doc(context.params.userId)
				.collection("orders")
				.doc(orderId);

      		batch.update(ordersRef, { status: "Paid", paymentId: paymentId });

			const restaurantRef = db.collection("restaurant_metadata")
				.doc("eastylian")

			batch.update(restaurantRef, {
				totalOrders: admin.firestore.FieldValue.increment(1),
				totalSalesAmount: admin.firestore.FieldValue.increment(amount)
			})
				
		} else {
			const paymentsRef = db
				.collection("users")
				.doc(context.params.userId)
				.collection("payments")
				.doc(paymentId);

			batch.update(paymentsRef, { status: "require_confirmation" });
		}

   		return await batch.commit();
  	});


exports.createModeratorOnAuth = functions.auth.user().onCreate(async (user) => {
	var isModerator = false
	if (user.phoneNumber != null) {
		if (user.phoneNumber == "+916767676767") {
		console.log("The phone number " + user.phoneNumber + " is in the list" + phoneNumbers)
		isModerator = true
		} else {
		console.log("The phone number " + user.phoneNumber + " is not in the list " + phoneNumbers)
		isModerator = false
		}
	}

	const customClaims = {
		admin: isModerator
	};

	try {
		// Set custom user claims on this newly created user.
		await admin.auth().setCustomUserClaims(user.uid, customClaims);
	} catch (error) {
		console.log(error);
	}
});

exports.updateLocationsOnOrderPlaced = functions.firestore.document("users/{userId}/orders/{orderId}")
	.onCreate(async (snap, context) => {
		if (snap == null) {
			return;
		}

		const placeObj = snap.get("place");

		const place = {
			id: placeObj.id,
			name: placeObj.name,
			latitude: placeObj.latitude,
			longitude: placeObj.longitude,
			address: placeObj.address,
			createdAt: Date.now()
		}

		const placesRef = db.collection("users")
			.doc(context.params.userId)
			.collection("places")
			.doc(place.id);
		
		return await placesRef.set(place);
	});


exports.initiateRefund = functions.firestore.document("users/{userId}/refunds/{refundId}")
	.onCreate(async (snap, context) => {
		if (snap == null)
			return;

		const paymentId = snap.get("paymentId");
		const orderId = snap.get("orderId");
		const receiverId = snap.get("receiverId");
		const amount = snap.get("amount");
		
		try {
			const response = await axios.post(`https://${razorpayKeyId}:${secret_key}@api.razorpay.com/v1/payments/${paymentId}/refund`);
			console.log(response);

			const batch = db.batch();
			batch.update(snap.ref, {status: response.data.status});

			const restaurantRef = db.collection("restaurant_metadata").doc("eastylian");

			batch.update(restaurantRef, {
				totalRefunds: admin.firestore.FieldValue.increment(1),
				totalRefundAmount: admin.firestore.FieldValue.increment(amount),
				totalSalesAmount: admin.firestore.FieldValue.increment(-amount) 
			});

			const orderRef = db.collection("users").doc(receiverId).collection("orders").doc(orderId);

			batch.update(orderRef, {status: "Cancelled"});

			await batch.commit();

			return this.sendNotification({status: "Cancelled", uid: receiverId}, context);
		} catch (error) {
			console.log(error.response.body);
			return;
		}
	});

exports.createModerator = functions.https.onCall(async (data, context) => {
	if (context.auth.token.admin !== true) {
		return {
			reponse: "Permission denied. User is not an admin."
		};
	}

	const phone = data.phone;
	if (!phone) {
		return {
			response: "please include user's phone number"
		};
	}

	const user = await admin.auth().getUserByPhoneNumber(phone);
	if (!user) {
		return {
			reponse: `no user founf for ${phone}`
		};
	}

	const uid = user.uid;

	await admin.auth().setCustomUserClaims(uid, {admin: true});

	return {
		response: `${phone} is now a moderator`
	}
});

// exports.sendNotification = async (data) => {
// 	const receiverId = data.uid;

// 	const user = await db.collection("users").doc(receiverId).get();
	
// 	if (!user.exists) {
// 		console.log('No such document!');
// 		return {
// 			response: "There is no such user in database"
// 		};
// 	} 

// 	const token = user.get("registrationToken");

// 	const orderStatus = data.status;

// 	console.log("User token - " + token + ", Status -> " + orderStatus);

// 	var notificationTitle = "";
// 	var msg = "";
// 	if (orderStatus == "Preparing") {
// 		notificationTitle = "Order started";
// 		msg = "We have started making your cake.";
// 	} else if (orderStatus == "Delivering") {
// 		notificationTitle = "We are arriving at your doorstep soon.";
// 		msg = "Get ready to receive a load of happiness.";
// 	} else if (orderStatus == "Delivered") {
// 		notificationTitle = "Enjoy your cake.";
// 		msg = "Be sure to give us feedback. We will always listen to you.";
// 	} else if (orderStatus == "Cancelled") {
// 		notificationTitle = "Uh-Oh! Sorrrry! Cannot deliver this one.";
// 		msg = "Don't worry if money was deducted from your account, it will be refunded soon.";
// 	} else {
// 		notificationTitle = "Brrrr";
// 		msg = "Something went wrong. Contact the developer.";
// 	}

// 	const payload = {
//         notification: {
//           title: notificationTitle,
//           body: msg,
// 		  sound : "default"
//         }
//     };

// 	return await messaging().sendToDevice(token, payload, {priority: 'high'});
// };

// exports.sendNotificationOnOrderStatusUpdate = functions.https.onCall(async (data, context) => {
// 	if (context.auth === null) {
// 		return {
// 			reponse: "Permission denied. Request from a client app."
// 		};
// 	}

// 	return this.sendNotification({status: data.status, uid: data.uid});
// });

exports.sendNotificationByAdmin = async (data) => {
	const notificationTitle = data.title;
	const msg = data.content;

	console.log(`${notificationTitle} : ${msg}`);

	const payload = {
        notification: {
          title: notificationTitle,
          body: msg,
		  sound: 'default'
        }
    };

	// Send a message to devices subscribed to the provided topic.
	try {
		const response = await admin.messaging().sendToTopic("general", payload, { priority: 'high' });
		// Response is a message ID string.
		console.log('Successfully sent message:', response);
	} catch (error) {
		console.log('Error sending message:', error);
	}

	// return await admin.messaging().sendToTopic("general", payload);
}


exports.addRegistrationToken = functions.https.onCall(async (data, context) => {
	
	if (context.auth != null) {
		return {
			reponse: "Permission denied. Request from a client app."
		};
	}

	const restaurantReference = db.collection("restaurant_metadata").doc("eastylian");
	const userRegistrationToken = data.userRegistrationToken;

	const restaurantData = await restaurantReference.get();
	const currentRegistrationDocumentId = restaurantData.data().currentRegistrationDocumentId;

	const FieldValue = admin.firestore.FieldValue;

	const currentRegistrationDocumentRef = db.collection("registration_tokens")
	.doc(currentRegistrationDocumentId);


	const currentRegistrationDocument = await currentRegistrationDocumentRef.get();

	const count = currentRegistrationDocument.data().count;
	
	if (count <= 3000) {
		return await currentRegistrationDocumentRef.update(
			{
				registrationTokens: FieldValue.arrayUnion(userRegistrationToken), 
				count: FieldValue.increment(1)
			}
		);
	} else {

		const newDocRef = db.collection("registration_tokens").doc();
		const nextRegistrationDocumentId = newDocRef.id;

		const newDoc = {
			registrationTokens: [userRegistrationToken],
			count: 0
		};

		await newDocRef.set(newDoc);

		return await restaurantReference.update({currentRegistrationDocumentId: nextRegistrationDocumentId});

	}
	
});


exports.listenOnNotifications = functions.firestore.document("notifications/{notificationId}")
	.onCreate(async (snap, context) => {

		const title = snap.get("title");
		const body = snap.get("content");

		return this.sendNotificationByAdmin({
			title: title,
			content:body
		});

	});



exports.onOrderUpdate = functions.firestore.document("users/{userId}/orders/{orderId}")
	.onUpdate(async (change, context) => {
		const userId = context.params.userId;
		const orderId = context.params.orderId;

		const orderStatus = change.after.get("status");

		const userSnap = await db.collection("users").doc(userId).get();
		const userRegistrationToken = userSnap.get("registrationToken");

		var notificationTitle = "";
		var msg = "";
		if (orderStatus == "Preparing") {
			notificationTitle = "Order started";
			msg = "We have started making your cake.";
		} else if (orderStatus == "Delivering") {
			notificationTitle = "We are arriving at your doorstep soon.";
			msg = "Get ready to receive a load of happiness.";
		} else if (orderStatus == "Delivered") {
			notificationTitle = "Enjoy your cake.";
			msg = "Be sure to give us feedback. We will always listen to you.";
		} else if (orderStatus == "Cancelled") {
			notificationTitle = "Uh-Oh! Sorrrry! Cannot deliver this one.";
			msg = "Don't worry if money was deducted from your account, it will be refunded soon.";
		} else {
			return {
				reponse: "Order is just created."
			}
		}

		const payload = {
			data: {
				title: notificationTitle,
				body: msg
			},
			notification: {
			  title: notificationTitle,
			  body: msg,
			  sound: 'default'
			}
		};

		console.log(orderId + " status -> " + orderStatus);
		
		return await admin.messaging().sendToDevice(userRegistrationToken, payload, {priority: 'high'});
	});