package com.timepath.steam.webapi

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.ActionListener
import javax.swing.*

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

    public val captchaInput: JTextField = JTextField().let {
        it.setMinimumSize(Dimension(120, 28))
        it
    }
    public val messageLabel: JLabel = JLabel("message")
    public val passInput: JPasswordField = JPasswordField("pass").let {
        it.setMinimumSize(Dimension(120, 28))
        it.addActionListener(ActionListener { login() })
        it
    }
    public val steamguardInput: JTextField = JTextField().let {
        it.setMinimumSize(Dimension(120, 28))
        it.addActionListener(ActionListener { login() })
        it
    }
    public val userInput: JTextField = JTextField("user").let {
        it.setMinimumSize(Dimension(120, 28))
        it
    }
    public val captchaLabel: JLabel = JLabel("captcha")

    init {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        getContentPane().add(JPanel().let {
            it.setLayout(BorderLayout())
            it.add(JPanel().let {
                it.setLayout(BorderLayout())
                it.add(JPanel().let {
                    it.setLayout(BoxLayout(it, BoxLayout.LINE_AXIS))
                    it.add(userInput)
                    it.add(passInput)
                    it
                }, BorderLayout.PAGE_START)
                it.add(JPanel().let {
                    it.setLayout(BoxLayout(it, BoxLayout.LINE_AXIS))
                    it.add(JLabel("steamguard"))
                    it.add(steamguardInput)
                    it
                }, BorderLayout.CENTER)
                it.add(JPanel().let {
                    it.setLayout(BoxLayout(it, BoxLayout.LINE_AXIS))
                    it.add(captchaLabel)
                    it.add(captchaInput)
                    it
                }, BorderLayout.PAGE_END)
                it
            }, BorderLayout.PAGE_START)
            it.add(messageLabel, BorderLayout.CENTER)
            it.add(JButton("login").let {
                it.addActionListener(ActionListener { login() })
                it
            }, BorderLayout.PAGE_END)
            it
        }, BorderLayout.CENTER)
        pack()
    }

    public abstract fun login()
}
