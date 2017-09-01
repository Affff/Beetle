/**
 * Created by Afff on 01.09.2017.
 */

function getContentRoots() {
    return ["client/info"];
}

function run(context) {
    return {
        code: 200,
		data: 'host: ' + context.getHost() + ", ip: " + context.getIp()
    };
}