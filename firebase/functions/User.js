module.exports =  class User {

    userId = "";
    name = "";
    phoneNo = "";
    lastPlace = {
        id: "",
        name: "",
        latitude: 0.0,
        longitude: 0.0,
        address: ""
    };
    balance = 0.0;
    email = "";
    photoUrl = "";

    constructor(p0, p1, p2, p3, p4, p5, p6) {
        this.userId = p0;
        this.name = p1;
        this.phoneNo = p2;
        this.lastPlace = p3;
        this.balance = p4;
        this.email = p5;
        this.photoUrl = p6;
    }
}