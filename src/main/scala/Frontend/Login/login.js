
function login(){
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const request = new XMLHttpRequest();

    request.onreadystatechange = function(){
        if (this.readyState === 4 && this.status === 200){
            window.location.href = "/";
            // Do something with the response
            //console.log(this.response);
        }else{
            console.log(this.response);
            const serverResponseElem = document.getElementById("ServerResponse");
            serverResponseElem.innerHTML = this.response;
            serverResponseElem.style = "display: inline;";
        }
    }
    request.open("POST", "/login");
    const messageObject = {"username": username, "password": password};
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

