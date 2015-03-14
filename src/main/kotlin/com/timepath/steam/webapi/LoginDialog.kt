package com.timepath.steam.webapi


import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
public abstract class LoginDialog
/**
 * Creates new form LoginDialog
 *
 * @param parent
 * @param modal
 */
protected(parent: Frame?, modal: Boolean) : JDialog(parent, modal) {

    public var captchaInput: JTextField
        private set
    public var captchaLabel: JLabel
        private set
    public var loginButton: JButton
        private set
    public var messageLabel: JLabel
        private set
    public var passInput: JPasswordField
        private set
    public var steamguardInput: JTextField
        private set
    public var userInput: JTextField
        private set

    {
        val jPanel6 = JPanel()
        val jPanel7 = JPanel()
        val jPanel4 = JPanel()
        $userInput = JTextField()
        $passInput = JPasswordField()
        val jPanel1 = JPanel()
        val jLabel1 = JLabel()
        $steamguardInput = JTextField()
        val captchaPanel = JPanel()
        $captchaLabel = JLabel()
        $captchaInput = JTextField()
        $messageLabel = JLabel()
        $loginButton = JButton()
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        jPanel6.setLayout(BorderLayout())
        jPanel7.setLayout(BorderLayout())
        jPanel4.setLayout(BoxLayout(jPanel4, BoxLayout.LINE_AXIS))
        userInput.setText("user")
        userInput.setMinimumSize(Dimension(120, 28))
        jPanel4.add(userInput)
        passInput.setText("pass")
        passInput.setMinimumSize(Dimension(120, 28))
        passInput.addActionListener(object : ActionListener {
            override fun actionPerformed(evt: ActionEvent) {
                login()
            }
        })
        jPanel4.add(passInput)
        jPanel7.add(jPanel4, BorderLayout.NORTH)
        jPanel1.setLayout(BoxLayout(jPanel1, BoxLayout.LINE_AXIS))
        jLabel1.setText("steamguard")
        jPanel1.add(jLabel1)
        steamguardInput.setMinimumSize(Dimension(120, 28))
        jPanel1.add(steamguardInput)
        jPanel7.add(jPanel1, BorderLayout.CENTER)
        captchaPanel.setLayout(BoxLayout(captchaPanel, BoxLayout.LINE_AXIS))
        captchaLabel.setText("captcha")
        captchaPanel.add(captchaLabel)
        captchaInput.setMinimumSize(Dimension(120, 28))
        captchaPanel.add(captchaInput)
        jPanel7.add(captchaPanel, BorderLayout.SOUTH)
        jPanel6.add(jPanel7, BorderLayout.PAGE_START)
        messageLabel.setText("message")
        jPanel6.add(messageLabel, BorderLayout.CENTER)
        loginButton.setText("login")
        loginButton.addActionListener(object : ActionListener {
            override fun actionPerformed(evt: ActionEvent) {
                login()
            }
        })
        jPanel6.add(loginButton, BorderLayout.SOUTH)
        getContentPane().add(jPanel6, BorderLayout.CENTER)
        pack()
    }

    public abstract fun login()

    class object {

        /**
         * @param args the command line arguments
         */
        public platformStatic fun main(args: Array<String>): Unit = SwingUtilities.invokeLater {
            val dialog = object : LoginDialog(JFrame(), true) {
                override fun login() = Unit
            }
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
            dialog.setVisible(true)
        }
    }
}
