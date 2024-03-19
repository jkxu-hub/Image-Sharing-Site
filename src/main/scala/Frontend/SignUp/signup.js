//TODO A function that checks if the password matches requirements

// A function that gets the form input from user,  makes and listens for an HTTP request
function signUp(){
    const username = document.getElementById("username").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const request = new XMLHttpRequest();

    request.onreadystatechange = function(){
        if (this.readyState === 4 && this.status === 200){
            window.location.href = "/signupSuccess";
            // Do something with the response
            //console.log(this.response);
        }else{
            console.log(this.response);
            const serverResponseElem = document.getElementById("ServerResponse");
            serverResponseElem.innerHTML = this.response;
            serverResponseElem.style = "display: inline;";
        }
    }
    request.open("POST", "/signup");
    const messageObject = {"username": username, "email": email, "password": password};
    request.send(JSON.stringify(messageObject));
}

function togglePasswordVisibility() {
    const passwordBox = document.getElementById("password");
    if (passwordBox.type === "password") {
        passwordBox.type = "text";
    } else {
        passwordBox.type = "password";
    }
}
