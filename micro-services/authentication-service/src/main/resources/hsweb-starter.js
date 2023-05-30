//组件信息
var info = {
    groupId: "org.jetlinks.project",
    artifactId: "authentication-service",
    version: "1.0.0",
    website: "https://github.com/jetlinks",
    comment: "JetLinks"
};

var users = [{
    "id" : "1199596756811550720",
    "username" : "admin",
    "password": "104ffe90cd840e08f7a79c7fddbe1699",
    "salt": "LmKOhcoB",
    "name": "超级管理员",
    "status":1
}];
//版本更新信息
var versions = [
    {
        version: "3.0.0",
        upgrade: function (context) {

        }
    }
];

function initialize(context) {
    var database = context.database;

    database.dml().upsert("s_user").values(users).execute().sync();
}

function install(context) {


}


//设置依赖
dependency.setup(info)
    .onInstall(install)
    .onUpgrade(function (context) { //更新时执行
        var upgrader = context.upgrader;
        upgrader.filter(versions)
            .upgrade(function (newVer) {
                newVer.upgrade(context);
            });
    })
    .onUninstall(function (context) { //卸载时执行

    }).onInitialize(initialize);