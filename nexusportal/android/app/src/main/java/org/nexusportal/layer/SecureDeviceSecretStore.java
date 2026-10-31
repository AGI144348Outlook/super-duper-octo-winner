package org.nexusportal.layer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

/** Encrypts the device capability locally with a non-exportable Android Keystore key. */
final class SecureDeviceSecretStore {
 private static final String ALIAS="nexusportal.device.secret.v1";
 private static final String PREFS="nexusportal_secure_device";
 private SecureDeviceSecretStore(){}
 private static SecretKey key() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
  KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
   .setRandomizedEncryptionRequired(true).build());
  return g.generateKey();
 }
 static void put(Context c,String name,String value) throws Exception {
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
  byte[] iv=cipher.getIV(),data=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
  ByteBuffer b=ByteBuffer.allocate(4+iv.length+data.length);b.putInt(iv.length).put(iv).put(data);
  c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(name,Base64.encodeToString(b.array(),Base64.NO_WRAP)).apply();
 }
 static String get(Context c,String name) {
  try {
   String encoded=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(name,null);if(encoded==null)return null;
   ByteBuffer b=ByteBuffer.wrap(Base64.decode(encoded,Base64.NO_WRAP));int n=b.getInt();if(n<12||n>32)return null;
   byte[] iv=new byte[n],data=new byte[b.remaining()-n];b.get(iv).get(data);
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
   return new String(cipher.doFinal(data),StandardCharsets.UTF_8);
  } catch(Exception e){return null;}
 }
 static void remove(Context c,String name){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(name).apply();}
 static void clear(Context c){
  c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();
  try{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(ks.containsAlias(ALIAS))ks.deleteEntry(ALIAS);}catch(Exception ignored){}
 }
}
