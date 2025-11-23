package ChatClient;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import ChatClient.chatserver.models.Message;
import ChatClient.chatserver.models.User;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class ChatMenu extends JFrame
{
    private final Client client;
    private JPanel conversationPanel;
    private JPanel userListContainer;
    private int currentRecipientId = -1;

    private enum SendMode { UNICAST, MULTICAST, BROADCAST }
    private SendMode currentMode = SendMode.UNICAST;
    private List<User> multicastTargets = null;

    public ChatMenu(Client client)
    {
        this.client = client;

        client.setIncomingMessageListener(m -> {
            SwingUtilities.invokeLater(() -> {
                if (m.getSenderId() == currentRecipientId)
                {
                    spawnMessageUI(m);
                    scrollToBottom();
                }
            });
        });

        setTitle("Chat Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(280);
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        //---------------------------
        // SIDEBAR
        //---------------------------
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        splitPane.setLeftComponent(sidebar);

        JPanel tabPanel = new JPanel(new GridLayout(1,2));
        JButton activeBtn = new JButton("Active Users");
        JButton allBtn = new JButton("All Users");

        styleTabButton(activeBtn, true);
        styleTabButton(allBtn, false);

        tabPanel.add(activeBtn);
        tabPanel.add(allBtn);
        sidebar.add(tabPanel, BorderLayout.NORTH);

        userListContainer = new JPanel();
        userListContainer.setLayout(new BoxLayout(userListContainer, BoxLayout.Y_AXIS));
        userListContainer.setBackground(Color.WHITE);
        JScrollPane userScroll = new JScrollPane(userListContainer);
        userScroll.setBorder(new EmptyBorder(10, 10, 10, 10));
        sidebar.add(userScroll, BorderLayout.CENTER);
        loadActiveUsers();
        activeBtn.addActionListener(e -> {
            styleTabButton(activeBtn, true);
            styleTabButton(allBtn, false);
            loadActiveUsers();
        });
        allBtn.addActionListener(e -> {
            styleTabButton(activeBtn, false);
            styleTabButton(allBtn, true);
            loadAllUsers();
        });

        JPanel conversationContainer = new JPanel(new BorderLayout());
        conversationContainer.setBackground(new Color(248, 248, 248));
        splitPane.setRightComponent(conversationContainer);
        JLabel conversationTitle = new JLabel("Conversation", SwingConstants.CENTER);
        conversationTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        conversationTitle.setBorder(new EmptyBorder(15, 0, 15, 0));
        conversationContainer.add(conversationTitle, BorderLayout.NORTH);
        conversationPanel = new JPanel();
        conversationPanel.setLayout(new BoxLayout(conversationPanel, BoxLayout.Y_AXIS));
        conversationPanel.setBackground(new Color(248, 248, 248));

        JScrollPane scrollPane = new JScrollPane(conversationPanel);
        scrollPane.setBorder(new EmptyBorder(20, 40, 20, 40));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        conversationContainer.add(scrollPane, BorderLayout.CENTER);

        // Input + split send button
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        JTextField inputField = new JTextField();

        JPanel sendPanel = new JPanel(new BorderLayout());
        JButton sendButton = new JButton("Send");
        sendButton.setBackground(new Color(120, 90, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        sendPanel.add(sendButton, BorderLayout.CENTER);

        JButton modeButton = new JButton("▼");
        modeButton.setPreferredSize(new Dimension(45, sendButton.getPreferredSize().height));
        modeButton.setBackground(new Color(110, 80, 245));
        modeButton.setForeground(Color.WHITE);
        modeButton.setFocusPainted(false);
        sendPanel.add(modeButton, BorderLayout.EAST);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendPanel, BorderLayout.EAST);
        conversationContainer.add(inputPanel, BorderLayout.SOUTH);

        // Popup menu for modes
        JPopupMenu modeMenu = new JPopupMenu();
        JMenuItem unicastItem = new JMenuItem("Unicast (default)");
        JMenuItem multicastItem = new JMenuItem("Multicast");
        JMenuItem broadcastItem = new JMenuItem("Broadcast");
        modeMenu.add(unicastItem);
        modeMenu.add(multicastItem);
        modeMenu.add(broadcastItem);

        modeButton.addActionListener(e -> modeMenu.show(modeButton, 0, modeButton.getHeight()));

        unicastItem.addActionListener(e -> {
            currentMode = SendMode.UNICAST;
            multicastTargets = null;
            if (currentRecipientId != -1) loadConversation(currentRecipientId);
        });

        broadcastItem.addActionListener(e -> {
            currentMode = SendMode.BROADCAST;
            multicastTargets = null;
            showModeMessage("YOU ARE ABOUT TO BROADCAST THIS TO ALL USERS");
        });

        multicastItem.addActionListener(e -> {
            currentMode = SendMode.MULTICAST;
            // fetch users and show selector (server order preserved)
            new Thread(() -> {
                try {
                    List<User> users = client.GetAllUsers();
                    SwingUtilities.invokeLater(() -> {
                        multicastTargets = openMulticastSelector(users);
                        int count = multicastTargets != null ? multicastTargets.size() : 0;
                        showModeMessage("YOU ARE ABOUT TO MULTICAST THIS TO " + count + " PEOPLE");
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> showModeMessage("FAILED TO LOAD USERS"));
                }
            }).start();
        });

        // send action
        ActionListener sendAction = e -> {
            String txt = inputField.getText().trim();
            if (txt.isEmpty()) return;

            try {
                switch (currentMode)
                {
                    case UNICAST:
                        if (currentRecipientId == -1) return;
                        client.SendMessage(txt, client.UserId, currentRecipientId);
                        loadConversation(currentRecipientId);
                        break;

                    case BROADCAST:
                        List<User> all = client.GetAllUsers();
                        for (User u : all) {
                            if (u.getId() != client.UserId) {
                                client.SendMessage(txt, client.UserId, u.getId());
                            }
                        }
                        inputField.setText("");
                        showModeMessage("MESSAGE BROADCASTED");
                        break;

                    case MULTICAST:
                        if (multicastTargets == null || multicastTargets.isEmpty()) return;
                        for (User u : multicastTargets) {
                            client.SendMessage(txt, client.UserId, u.getId());
                        }
                        inputField.setText("");
                        showModeMessage("MESSAGE SENT TO " + multicastTargets.size() + " PEOPLE");
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            inputField.setText("");
            scrollToBottom();
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        setVisible(true);
    }

    private void styleTabButton(JButton btn, boolean active)
    {
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));

        if (active)
        {
            btn.setBackground(new Color(120, 90, 255));
            btn.setForeground(Color.WHITE);
        }
        else
        {
            btn.setBackground(new Color(230, 230, 230));
            btn.setForeground(Color.BLACK);
        }
    }

    private void loadAllUsers()
    {
        new Thread(() -> {
            try {
                List<User> users = client.GetAllUsers();
                SwingUtilities.invokeLater(() -> {
                    userListContainer.removeAll();
                    for (User u : users)
                    {
                        if (u.getId() != client.UserId)
                            userListContainer.add(createUserItem(u));
                    }
                    userListContainer.revalidate();
                    userListContainer.repaint();
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadActiveUsers()
    {
        new Thread(() -> {
            try {
                List<User> users = client.GetActiveUsers();
                SwingUtilities.invokeLater(() -> {
                    userListContainer.removeAll();
                    for (User u : users)
                    {
                        if (u.getId() != client.UserId)
                            userListContainer.add(createUserItem(u));
                    }
                    userListContainer.revalidate();
                    userListContainer.repaint();
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("something is happening here");
            }
        }).start();
    }

    private JPanel createUserItem(User user)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.WHITE);
        JLabel nameLabel = new JLabel(user.getUserName());
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        panel.add(nameLabel, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                currentRecipientId = user.getId();
                // when switching back to unicast, reset mode to unicast for clarity
                if (currentMode == SendMode.UNICAST || currentMode == SendMode.MULTICAST) {
                    currentMode = SendMode.UNICAST;
                }
                loadConversation(currentRecipientId);
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt)
            {
                panel.setBackground(new Color(240, 240, 240));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt)
            {
                panel.setBackground(Color.WHITE);
            }
        });
        return panel;
    }

    private JPanel createMessageBubble(Message msg, boolean isMine)
    {
        final int BUBBLE_WIDTH = 260;
        final int BUBBLE_HEIGHT = 60;

        JPanel bubble = new JPanel(){
            @Override
            protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isMine ? new Color(120,90,255) : new Color(230,230,230));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(8, 15, 8, 15));

        Dimension fixedSize = new Dimension(BUBBLE_WIDTH, BUBBLE_HEIGHT);
        bubble.setPreferredSize(fixedSize);
        bubble.setMaximumSize(fixedSize);
        bubble.setMinimumSize(fixedSize);

        JLabel lbl = new JLabel("<html><body style='width:" + (BUBBLE_WIDTH - 30) + "px;'>" + msg.getContent() + "</body></html>");
        lbl.setForeground(isMine ? Color.WHITE : Color.BLACK);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setAlignmentX(isMine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        bubble.add(lbl);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        String rawTimestamp = msg.getTimestamp(); // e.g. "2025-11-18T20:26:42.877765"
        String formatted = rawTimestamp.replace('T', ' ');
        int dotIndex = formatted.indexOf('.');
        if(dotIndex != -1) formatted = formatted.substring(0, dotIndex);

        JLabel timeLabel = new JLabel(formatted);
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        timeLabel.setForeground(isMine ? Color.GRAY : Color.BLACK);
        timeLabel.setVisible(false);
        bottomPanel.add(timeLabel, BorderLayout.WEST);

        if (isMine) {
            String status = msg.isSeen() ? "✔✔" : msg.isDelivered() ? "✔" : "";
            JLabel statusLabel = new JLabel(status);
            statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            statusLabel.setForeground(msg.isSeen() ? Color.CYAN : Color.BLACK);
            bottomPanel.add(statusLabel, BorderLayout.EAST);
        }
        bubble.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                timeLabel.setVisible(true);
                bubble.revalidate();
                bubble.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                timeLabel.setVisible(false);
                bubble.revalidate();
                bubble.repaint();
            }
        });

        bubble.add(Box.createVerticalStrut(5));
        bubble.add(bottomPanel);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.setOpaque(false);

        if (isMine) {
            container.add(Box.createHorizontalGlue());
            container.add(bubble);
        } else {
            container.add(bubble);
            container.add(Box.createHorizontalGlue());
        }

        return container;
    }

    private void spawnMessageUI(Message msg)
    {
        boolean mine = msg.getSenderId() == client.UserId;
        conversationPanel.add(createMessageBubble(msg, mine));
        conversationPanel.add(Box.createVerticalStrut(5));
        conversationPanel.revalidate();
    }

    private void loadConversation(int recipientId)
    {
        new Thread(() -> {
            try
            {
                List<Message> messages = client.getConversation(recipientId);
                client.SetSeen(recipientId);

                SwingUtilities.invokeLater(() -> {
                    conversationPanel.removeAll();

                    if (messages.isEmpty())
                    {
                        showPlaceholder("No messages yet...");
                    }
                    else
                    {
                        for (Message msg : messages)
                        {
                            spawnMessageUI(msg);
                        }
                    }

                    scrollToBottom();
                    conversationPanel.revalidate();
                    conversationPanel.repaint();
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();
    }

    private void showPlaceholder(String text)
    {
        JLabel placeholderLabel = new JLabel(text, SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        placeholderLabel.setForeground(Color.GRAY);

        JPanel wrapPanel = new JPanel(new GridBagLayout());
        wrapPanel.setBackground(new Color(248, 248, 248));
        wrapPanel.add(placeholderLabel);

        conversationPanel.add(wrapPanel);
        conversationPanel.add(Box.createVerticalGlue());
    }

    private void showModeMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            conversationPanel.removeAll();

            JLabel label = new JLabel(msg.toUpperCase(), SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 26));
            label.setForeground(Color.DARK_GRAY);

            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setBackground(new Color(248, 248, 248));
            wrap.add(label);

            conversationPanel.add(wrap);
            conversationPanel.revalidate();
            conversationPanel.repaint();
        });
    }

    private List<User> openMulticastSelector(List<User> users) {
        JDialog dialog = new JDialog(this, "Select Users", true);
        dialog.setSize(350, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        java.util.List<JCheckBox> checkBoxes = new java.util.ArrayList<>();
        for (User u : users) {
            if (u.getId() == client.UserId) continue;
            JCheckBox cb = new JCheckBox(u.getUserName());
            cb.putClientProperty("user", u);
            panel.add(cb);
            checkBoxes.add(cb);
        }

        JScrollPane scroll = new JScrollPane(panel);
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");

        okBtn.addActionListener(e -> dialog.dispose());
        cancelBtn.addActionListener(e -> {
            dialog.dispose();
            // clear selection if cancelled
            checkBoxes.clear();
        });

        JPanel footer = new JPanel();
        footer.add(okBtn);
        footer.add(cancelBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);

        java.util.List<User> selected = new java.util.ArrayList<>();
        for (JCheckBox cb : checkBoxes) {
            if (cb.isSelected()) selected.add((User) cb.getClientProperty("user"));
        }
        return selected;
    }

    private void scrollToBottom()
    {
        SwingUtilities.invokeLater(() -> {
            JScrollPane scroll = (JScrollPane) conversationPanel.getParent().getParent();
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> { new LoginPage(); });
    }
}
