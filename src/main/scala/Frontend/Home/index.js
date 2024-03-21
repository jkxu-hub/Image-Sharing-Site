function logout(){
    const request = new XMLHttpRequest();
    request.onreadystatechange = function(){
        if (this.readyState === 4 && this.status === 200){
            window.location.reload();
            // Do something with the response
            console.log(this.response);
        }else{
            alert("An error has occurred");
        }
    }
    request.open("DELETE", "/logout");
    request.send();
}