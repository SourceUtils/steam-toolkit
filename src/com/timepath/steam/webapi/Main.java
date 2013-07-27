package com.timepath.steam.webapi;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.timepath.web.api.base.Connection;
import com.timepath.web.api.base.RequestBuilder;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.json.JSONObject;

/**
 * http://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v1/
 * http://cs.rin.ru/forum/viewtopic.php?f=20&t=60547&start=0&sid=937cd736a46b29a2492fc8770b733bd6
 * https://steamcommunity.com/public/javascript/login.js
 * https://github.com/Jessecar96/SteamBot/blob/a7ef3f402f5b7e2527b6be82d9e8c770d43eb5a0/SteamTrade/SteamWeb.cs
 * <p/>
 * @author timepath
 */
public class Main {

    private final LoginDialog dlg;

    private String emailsteamid = "";

    private class SteamConnection extends Connection {

        public SteamConnection(String method) {
            super(method);
        }

        public String getBaseUrl() {
            return "https://steamcommunity.com/";
        }

        public String getUserAgent() {
            return "Java steam wrapper";
        }

    }

    /**
     *
     * @param u
     * @param p
     * <p/>
     * @return [0] = (byte[]) token, [1] = (String) timestamp
     * <p/>
     * @throws Exception
     */
    private Object[] encrypt(String u, byte[] p) throws Exception {
        Connection login = new SteamConnection("login/getrsakey");
        RequestBuilder rb = RequestBuilder.fromArray(new String[][] {{"username", u}});

        JSONObject ret = new JSONObject(login.postget(rb.toString()));
        BigInteger mod = new BigInteger(ret.getString("publickey_mod"), 16);
        BigInteger exp = new BigInteger(ret.getString("publickey_exp"), 16);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(mod, exp);
        RSAPublicKey key = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return new Object[] {cipher.doFinal(p), ret.getString("timestamp")};
    }

    private boolean login(final String u, final long g, final Object[] enc) throws Exception {
        final String captcha = dlg.getCaptchaInput().getText();
        final String eauth = dlg.getSteamguardInput().getText();
        final byte[] cipherData = (byte[]) enc[0];

        Connection login = new SteamConnection("login/dologin");
        final JSONObject ret = new JSONObject(login.postget(RequestBuilder.fromArray(
                    new Object[][] {
                {"password", Base64.encode(cipherData)},
                {"username", u},
                {"emailauth", eauth},
                {"captchagid", g},
                {"captcha_text", captcha},
                {"emailsteamid", emailsteamid},
                {"rsatimestamp", enc[1]},}).toString()));

            if(ret.getBoolean("success")) {
                RequestBuilder rb = new RequestBuilder();
                String trans = ret.getString("transfer_url");
                JSONObject arr = ret.getJSONObject("transfer_parameters");
                Object[] keys = arr.keySet().toArray();
                for(Object k : keys) {
                    String key = k.toString();
                    rb.append(key, arr.get(key).toString());
                }
                System.out.println(trans + "?" + rb.toString());
                return true;
            } else {
                dlg.getMessageLabel().setText(ret.getString("message"));
                if(ret.optBoolean("emailauth_needed")) {
                    emailsteamid = ret.getString("emailsteamid");
                } else if(ret.optBoolean("captcha_needed")) {
                    final long gid = ret.getLong("captcha_gid");
                    String address = "https://steamcommunity.com/public/captcha.php?gid=" + gid;
                    dlg.getCaptchaLabel().setIcon(new ImageIcon(ImageIO.read(
                            URI.create(address).toURL())));
                }
            }
            return false;
    }

    public Main() {
        dlg = new LoginDialog(null, true) {
            private Object[] enc;

            @Override
            public void login() {
                try {
                    String u = this.getUserInput().getText();
                    byte[] p = new String(this.getPassInput().getPassword()).getBytes();
                    if(enc == null) {
                        enc = encrypt(u, p);
                    }
                    boolean loggedin = login(u, -1, enc);
                    if(loggedin) {
                        this.dispose();
                        u = null;
                        Arrays.fill(p, 0, Math.min(0, p.length - 1), (byte) 0);
                        p = null;
                        return;
                    }
                } catch(Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Login failed");
            }
        };
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);

        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });
    }

    public static void main(String... args) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default look and
         * feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for(javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch(ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(LoginDialog.class.getName()).log(
                    java.util.logging.Level.SEVERE, null, ex);
        } catch(InstantiationException ex) {
            java.util.logging.Logger.getLogger(LoginDialog.class.getName()).log(
                    java.util.logging.Level.SEVERE, null, ex);
        } catch(IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LoginDialog.class.getName()).log(
                    java.util.logging.Level.SEVERE, null, ex);
        } catch(javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LoginDialog.class.getName()).log(
                    java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the dialog
         */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main();
            }
        });
    }

}
