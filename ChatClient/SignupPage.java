package ChatClient;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.function.Predicate;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class SignupPage extends JFrame
{
    static class ValidatableField
    {
        private final JTextField field;
        private final String placeholder;
        private final Predicate<String> validator;
        private final String errorMessage;

        public ValidatableField(JTextField field, String placeholder,
                                Predicate<String> validator, String errorMessage)
        {
            this.field = field;
            this.placeholder = placeholder;
            this.validator = validator;
            this.errorMessage = errorMessage;
            setupPlaceholder();
        }

        private void setupPlaceholder()
        {
            field.setForeground(new Color(150, 150, 150));
            field.setText(placeholder);

            field.addFocusListener(new FocusAdapter()
            {
                public void focusGained(FocusEvent e)
                {
                    if (field.getText().equals(placeholder))
                        {
                        field.setText("");
                        field.setForeground(Color.BLACK);
                        if (field instanceof JPasswordField)
                            // Set echo char to bullet for actual password input
                            ((JPasswordField) field).setEchoChar('•');
                    }
                }

                public void focusLost(FocusEvent e)
                {
                    if (field.getText().isEmpty())
                    {
                        field.setText(placeholder);
                        field.setForeground(new Color(150, 150, 150));
                        if (field instanceof JPasswordField)
                            ((JPasswordField) field).setEchoChar((char) 0);
                    }
                }
            });
        }

        public boolean isValidInput()
        {
            String text = field.getText().trim();
            if (text.equals(placeholder) || text.isEmpty() || !validator.test(text))
            {
                JOptionPane.showMessageDialog(field, errorMessage);
                field.requestFocus();
                return false;
            }
            return true;
        }

        public JTextField getField() { return field; }
        public String getValue()
        { 
            String text = field.getText().trim();
            // Return empty string if it's still the placeholder
            return text.equals(placeholder) ? "" : text; 
        }
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
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g);
        }
    }

    public SignupPage()
    {
        setTitle("Sign Up");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(new Color(248, 248, 252));
        setLayout(new GridBagLayout());

        JPanel card = new RoundedPanel(20, Color.WHITE);
        card.setPreferredSize(new Dimension(450, 420));
        card.setLayout(null);

        JLabel title = new JLabel("Welcome to Our App!", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBounds(0, 30, 450, 30);
        card.add(title);

        Border border = BorderFactory.createLineBorder(new Color(200, 200, 200));
        Border focus = BorderFactory.createLineBorder(new Color(120, 90, 250));

        FocusAdapter focusStyle = new FocusAdapter()
        {
            public void focusGained(FocusEvent e) 
        {
                ((JComponent)e.getSource()).setBorder(focus);
            }
            public void focusLost(FocusEvent e)
            {
                ((JComponent)e.getSource()).setBorder(border);
            }
        };

        ValidatableField firstName = new ValidatableField(
                new JTextField(), "First Name",
                text -> text.length() >= 2,
                "First name must be at least 2 characters long."
        );
        ValidatableField lastName = new ValidatableField(
                new JTextField(), "Last Name",
                text -> text.length() >= 2,
                "Last name must be at least 2 characters long."
        );
        ValidatableField username = new ValidatableField(
                new JTextField(), "Username",
                text -> text.length() >= 5,
                "Username must be at least 5 characters long."
        );
        ValidatableField password = new ValidatableField(
                new JPasswordField(), "Password",
                text -> text.length() >= 8,
                "Password must be at least 8 characters long."
        );
        ValidatableField confirm = new ValidatableField(
                new JPasswordField(), "Confirm Password",
                text -> true,
                "Please confirm your password."
        );

        JTextField[] fields = {firstName.getField(), lastName.getField(),
                               username.getField(), password.getField(), confirm.getField()};
        int[] yPositions = {90, 90, 140, 190, 240};

        for (int i = 0; i < fields.length; i++)
        {
            fields[i].setBorder(border);
            fields[i].setFont(new Font("Segoe UI", Font.PLAIN, 14));
            fields[i].setBackground(Color.WHITE);
            fields[i].addFocusListener(focusStyle);
            
            if (fields[i] instanceof JPasswordField)
                ((JPasswordField)fields[i]).setEchoChar((char)0);

            if (i == 0) fields[i].setBounds(60, yPositions[i], 160, 35); // First Name
            else if (i == 1) fields[i].setBounds(230, yPositions[i], 160, 35); // Last Name
            else fields[i].setBounds(60, yPositions[i], 330, 35); // Username, Password, Confirm

            card.add(fields[i]);
        }

        JButton confirmBtn = new JButton("Confirm");
        confirmBtn.setBounds(60, 295, 330, 40);
        confirmBtn.setBackground(new Color(120, 90, 250));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        confirmBtn.setFocusPainted(false);
        confirmBtn.setBorder(new EmptyBorder(0, 0, 0, 0));
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        confirmBtn.addActionListener(e ->
        {
            if (!firstName.isValidInput()) return;
            if (!lastName.isValidInput()) return;
            if (!username.isValidInput()) return;
            if (!password.isValidInput()) return;
            if (confirm.getValue().isEmpty())
            {
                JOptionPane.showMessageDialog(this, "Please confirm your password.");
                confirm.getField().requestFocus();
                return;
            }

            String pass = password.getValue();
            String conf = confirm.getValue();

            if (!pass.equals(conf))
            {
                JOptionPane.showMessageDialog(this, "Passwords do not match.");
                confirm.getField().requestFocus();
                return;
            }

            try 
            {
                Argon2 argon2 = Argon2Factory.create();
                String hashedPass = argon2.hash(2, 65536, 1, pass.toCharArray());
                Client client = new Client("localhost", 51102);
                boolean ok = client.SignUp(
                        firstName.getValue(),
                        lastName.getValue(),
                        username.getValue(),
                        hashedPass
                );
                System.out.println("Signup attempt for: " + username.getValue() + " was" + (ok ? " successful." : " unsuccessful."));
                if (ok)
                {
                    JOptionPane.showMessageDialog(this, "Signup Successful! You can now log in.");
                    new LoginPage().setVisible(true);
                    dispose();
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Account creation failed. An account may already exist with this username or name, or the server rejected the request.");
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Could not connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        card.add(confirmBtn);

        JLabel bottomText = new JLabel("Already have an account?");
        bottomText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bottomText.setBounds(100, 345, 200, 30);

        JLabel signIn = new JLabel("<html><a href='LoginPage.java'>Sign In</a></html>");
        signIn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        signIn.setForeground(new Color(120, 90, 250));
        signIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signIn.setBounds(270, 345, 100, 30);
        signIn.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                dispose();
                new LoginPage().setVisible(true);
            }
        });

        card.add(bottomText);
        card.add(signIn);

        add(card);
        setVisible(true);
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(SignupPage::new);
    }
}