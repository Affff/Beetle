/**
 * Created by Dilmukhamedov_A on 25.07.2017.
 */

function getContentRoots() {
    return ["serv/echo"];
}

function run(context) {
	return {
        code: 200,
		data: context.getParam('data')
    };
}