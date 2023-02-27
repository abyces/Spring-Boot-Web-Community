$(function(){
    $("#sendEmail").click(resetPassword);
});

function resetPassword() {
    // 使用jQuery获取内容
    var email = $("#your-email").val();
    console.log("in resetPassword func")
    // 发送异步请求
    $.post(
        CONTEXT_PATH + "/getAuthCode",
        {"email":email},
        function (data) {
            data = $.parseJSON(data);
            // 在提示框中显示返回的消息
            $("#hintBody").text(data.msg);

            // 显示提示框，2s后自动隐藏
            $("#hintModal").modal("show");
            setTimeout(function(){
                $("#hintModal").modal("hide");
            }, 2000);
        }
    );
}