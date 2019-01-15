package cordova.plugin.read.nfc;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class ReadNFC extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        // if (action.equals("readNFC")) {
        //     String message = args.getString(0);
        //     this.coolMethod(message, callbackContext);
        //     return true;
        // }
        if(action.equals("readNFC")) {
            this.readNFC(callbackContext);
            return true;
        }
        return false;
    }

    // private void coolMethod(String message, CallbackContext callbackContext) {
    //     if (message != null && message.length() > 0) {
    //         callbackContext.success(message);
    //     } else {
    //         callbackContext.error("Expected one non-empty string argument.");
    //     }
    // }
    
    private void readNFC(CallbackContext callbackContext) {
        try {
            callbackContext.success("This plugin reads NFC cards");
        } catch {
            callbackContext.error("Something went wrong.");
        }
    }
}
