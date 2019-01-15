var exec = require('cordova/exec');

// exports.coolMethod = function (arg0, success, error) {
//     exec(success, error, 'ReadNFC', 'coolMethod', [arg0]);
// };

module.exports.readNFC = function(arg0, success, error) {
    exec(success, error, 'ReadNFC', 'readNFC', [arg0]);
}
