package ChatClient;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class LoginPage extends JFrame
{
    private Client client;
    
    public LoginPage()
    {
        setTitle("Login Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(new Color(248, 248, 252));
        setLayout(new GridBagLayout());

        String[] hosts = {
            "brxdesktopapp.duckdns.org",
            "ip for lan set up",
            "localhost"
        };
        int port = 51102;
        boolean connected = false;
        for (String host : hosts)
        {
            try {
                client = new Client(host, port);
                connected = true;
                System.out.println("Connected to server at: " + host);
                break;
            } 
            catch (IOException e) {
                System.err.println("Failed to connect to: " + host);
            }
        }

        if (!connected)
        {
            JOptionPane.showMessageDialog(this,
                "Could not connect to any server.\n",
                "Connection Error",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        }

        JPanel card = new RoundedPanel(20, Color.WHITE);
        card.setPreferredSize(new Dimension(420, 380));
        card.setLayout(null);

        JLabel title = new JLabel("Welcome Back!", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBounds(0, 30, 420, 30);
        card.add(title);

        Border border = BorderFactory.createLineBorder(new Color(200, 200, 200));
        Border focus = BorderFactory.createLineBorder(new Color(120, 90, 250));

        JTextField userName = new JTextField();
        JPasswordField password = new JPasswordField();

        JTextField[] fields = { userName, password };
        String[] placeholders = { "Username", "Password" };

        FocusAdapter focusStyle = new FocusAdapter()
        {
            public void focusGained(FocusEvent e)
            {
                ((JComponent) e.getSource()).setBorder(focus);
            }
            public void focusLost(FocusEvent e)
            {
                ((JComponent) e.getSource()).setBorder(border);
            }
        };

        userName.setBounds(60, 100, 300, 40);
        password.setBounds(60, 160, 300, 40);

        for (int i = 0; i < fields.length; i++)
        {
            fields[i].setBorder(border);
            fields[i].setFont(new Font("Segoe UI", Font.PLAIN, 14));
            fields[i].setBackground(Color.WHITE);
            fields[i].addFocusListener(focusStyle);
            card.add(fields[i]);
            if (fields[i] instanceof JPasswordField)
                ((JPasswordField) fields[i]).setEchoChar((char) 0);
            addPlaceholder(fields[i], placeholders[i]);
        }

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(60, 220, 300, 40);
        loginButton.setBackground(new Color(120, 90, 250));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setFocusPainted(false);
        loginButton.setBorder(new EmptyBorder(0, 0, 0, 0));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        loginButton.addActionListener(e ->
        {
            String userNameText = userName.getText();
            String pass = new String(password.getPassword());
            try
            {
                if (client.LogIn(userNameText, pass))
                {
                    JOptionPane.showMessageDialog(this, "Login Successful!");
                    new ChatMenu(client).setVisible(true);
                    dispose();
                }
                else
                {
                    JOptionPane.showMessageDialog(this,
                        "Invalid User Name and password combination.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }

            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "An error occurred.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        card.add(loginButton);

        JLabel bottomText = new JLabel("Don't have an account?");
        bottomText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bottomText.setBounds(110, 280, 200, 30);

        JLabel signUp = new JLabel("<html><a href=''>Sign Up</a></html>");
        signUp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        signUp.setForeground(new Color(120, 90, 250));
        signUp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signUp.setBounds(255, 280, 100, 30);

        signUp.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                dispose();
                new SignupPage().setVisible(true);
            }
        });

        card.add(bottomText);
        card.add(signUp);
        add(card);
        setVisible(true);
    }

    static class RoundedPanel extends JPanel
    {
    private final int radius;
    private final Color bg;
    public RoundedPanel(int radius, Color bg)
    {
        this.radius = radius;
        this.bg = bg;
        setOpaque(false);
    }
    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}

private void addPlaceholder(JTextField field, String text)
{
        field.setForeground(new Color(150, 150, 150));
        field.setText(text);

        field.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                if (field.getText().equals(text))
                {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    if (field instanceof JPasswordField)
                    {
                        ((JPasswordField) field).setEchoChar('•');
                    }
                }
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                if (field.getText().isEmpty())
                {
                    field.setText(text);
                    field.setForeground(new Color(150, 150, 150));
                    if (field instanceof JPasswordField)
                    {
                        ((JPasswordField) field).setEchoChar((char) 0);
                    }
                }
            }
        });
    }
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(LoginPage::new);
    }
}