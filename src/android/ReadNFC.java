package cordova.plugin.read.nfc;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
// import org.apache.cordova.Plugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.util.*;
import android.nfc.Tag;
import android.app.Activity;
import android.app.PendingIntent;
import android.nfc.tech.TagTechnology;
import android.os.Parcelable;
// import com.google.common.primitives.UnsignedBytes;
import java.lang.reflect.*;
import java.util.*;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;

public class ReadNFC extends CordovaPlugin {
    public static final String ACTION = "readNFC";
    private Intent savedIntent = null;
    private PendingIntent pendingIntent = null;
    NfcAdapter nfcAdapter;
    Tag tag;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext){
        if(action.equals(ACTION)) {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this.cordova.getActivity());
            if(!nfcAdapter.isEnabled()) {
                callbackContext.error("NFC disabled");
            }
            createPendingIntent();
            nfcAdapter.disableForegroundDispatch(getActivity());
            nfcAdapter.enableForegroundDispatch(getActivity(), pendingIntent, null, null);
            Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techLists = tag.getTechList();
            if(techLists[1].equals("android.nfc.tech.MifareClassic")) {
                mifareClassicRead(tag, callbackContext);
            }else if(techLists[1].equals("android.nfc.tech.MifareUltralight")) {
                mifareUltraRead(tag, callbackContext);
            }       
            // callbackContext.success("123");
        }
        return true;
    }

    private static void mifareClassicRead(Tag tag, CallbackContext callbackContext) {
        // MifareClassic1k classic1k = null;
        JSONObject json = new JSONObject();
        String result="";
        String block4 = "";
        String block2 = "";
        MifareClassic mifare = MifareClassic.get(tag);
        if (mifare == null) {
            callbackContext.error("Something went wrong!");
        }
        try {
            mifare.connect();
            int blockCount = mifare.getBlockCount();
            int blockCountForSector = mifare.getBlockCountInSector(0);
            int sectorCount = mifare.getSectorCount();
            json.put("blockCount", blockCount);
            json.put("blkCountInSector", blockCountForSector);
            json.put("sectorCount", sectorCount);
            json.put("size", mifare.getSize());
            json.put("type", mifare.getType());
            json.put("firstBlkinSector0", mifare.sectorToBlock(0));
            
            if(mifare.isConnected()) {
                if(mifare.authenticateSectorWithKeyA(0, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY))
                    json.put("MAD", "true");
                else if(mifare.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT))
                    json.put("Default", "true");
                else if(mifare.authenticateSectorWithKeyA(0, MifareClassic.KEY_NFC_FORUM))
                    json.put("NFCforum", "true");
                else {
                    json.put("AuthDenied", "true");
                    // continue;
                }
                for(int k =0; k < mifare.getBlockCountInSector(0) - 1; ++k) {
                    int block = mifare.sectorToBlock(0) + k;
                    byte[] data = null;
                    try {
                        data = mifare.readBlock(block);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    String blockData = bytesToString(data);
                    switch (k) {
                        case 0:
                            json.put("block1", blockData);
                            break;
                        case 1:
                            block2 = blockData;
                            json.put("block2", blockData);
                            break;
                        case 2:
                            json.put("block3", blockData);
                            break;
                        case 3:
                            block4 = blockData;
                            json.put("block4", blockData);
                            break;
                    }
                }
            }
            result = block2.substring(0, 7);
            json.put("result", result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mifare.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        callbackContext.success(result);
    }

    private static void mifareUltraRead(Tag tag, CallbackContext callbackContext) {
        JSONObject json = new JSONObject();
        String stringID1, stringID2, result = "";

        MifareUltralight mifare = MifareUltralight.get(tag);
        if(mifare == null)
            callbackContext.error("Something went wrong!");
        
        try {
            mifare.connect();
            json.put("type", mifare.getType());
            if(mifare.isConnected()) {
                byte[] res = mifare.readPages(4);
                stringID1 = bytesToString(res);
                byte[] res2 = mifare.readPages(8);
                stringID2 = bytesToString(res2);
                if(stringID1.startsWith("CGPIX")) {
                    result = stringID1 + stringID2.substring(0, stringID2.indexOf(0X00));
                } else if (stringID1.endsWith("CGPIX")) {
                    result = stringID1.substring(0, 7);
                    if(stringID2.charAt(0) != 0X00){
                        result += stringID2.substring(0, 7);
                        if(stringID2.charAt(7) !=0X00) {
                            result += stringID2.substring(7, 16);
                        }
                    }
                }
            }
            json.put("result", result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mifare.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        callbackContext.success(result);
    }

    private static String bytesToString(byte[] ary) {
		final StringBuilder result = new StringBuilder();
		for(int i = 0; i < ary.length; ++i) {
			result.append(Character.valueOf((char)ary[i]));
		}
		return result.toString();
	}

    private Activity getActivity() {
        return this.cordova.getActivity();
    }

    private void createPendingIntent() {
        if (pendingIntent == null) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        savedIntent = intent;
    }

    private void setIntent(Intent intent) {
        this.cordova.getActivity().setIntent(intent);
    }
    private Intent getIntent() {
        return this.cordova.getActivity().getIntent();
    }
    
    // int parse_tag(Intent intent) {
    //     // if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
    //         try{
    //             Parcelable tag = savedIntent.getParcelableExtra("android.nfc.extra.TAG");
    //             Field f = tag.getClass().getDeclaredField("mId");
    //             f.setAccessible(true);

    //             byte[] mId = (byte[]) f.get(tag);
    //             StringBuilder sb = new StringBuilder();
    //             int l = mId.length;
    //             // for (byte id : mId) {
    //             //     String hexString = Integer.toHexString(UnsignedBytes.toInt(id));
    //             //     if (hexString.length() == 1) sb.append("0");
    //             //     sb.append(hexString);
    //             // }
                
    //             return l;
    //         }

    //         catch(Exception e){
    //             e.printStackTrace();
    //         }
    //     // }
    //     return 100;
    // }

}
