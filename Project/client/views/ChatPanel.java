package Project.client.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import Project.client.Card;
import Project.client.Client;
import Project.client.ClientUtils;
import Project.client.ICardControls;

public class ChatPanel extends JPanel {
    private static Logger logger = Logger.getLogger(ChatPanel.class.getName());
    private JPanel chatArea = null;
    private UserListPanel userListPanel;
    private ArrayList<String> chatHistory = new ArrayList<>(); // Chat history storage


    public ChatPanel(ICardControls controls) {
        super(new BorderLayout(10, 10));
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentY(Component.BOTTOM_ALIGNMENT);
         // Load chat history when the panel is created
         loadChatHistory();
        // wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        // no need to add content specifically because scroll wraps it
        wrapper.add(scroll);
        this.add(wrapper, BorderLayout.CENTER);

        JPanel input = new JPanel();
        input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
        JTextField textValue = new JTextField();
        input.add(textValue);
        JButton button = new JButton("Send");
        JButton exportButton = new JButton("Export Chat");
exportButton.addActionListener(event -> exportChatHistory());
input.add(exportButton);
        // lets us submit with the enter key instead of just the button click
        textValue.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    button.doClick();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }

        });
        button.addActionListener((event) -> {
            try {
                String text = textValue.getText().trim();
                if (text.length() > 0) {
                    Client.INSTANCE.sendMessage(text);
                    textValue.setText("");// clear the original text

                    // debugging
                    logger.log(Level.FINEST, "Content: " + content.getSize());
                    logger.log(Level.FINEST, "Parent: " + this.getSize());

                }
            } catch (NullPointerException e) {
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        chatArea = content;
        input.add(button);
        userListPanel = new UserListPanel(controls);
        this.add(userListPanel, BorderLayout.EAST);
        this.add(input, BorderLayout.SOUTH);
        this.setName(Card.CHAT.name());
        controls.addPanel(Card.CHAT.name(), this);
        chatArea.addContainerListener(new ContainerListener() {

            @Override
            public void componentAdded(ContainerEvent e) {

                if (chatArea.isVisible()) {
                    chatArea.revalidate();
                    chatArea.repaint();
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (chatArea.isVisible()) {
                    chatArea.revalidate();
                    chatArea.repaint();
                }
            }

        });
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // System.out.println("Resized to " + e.getComponent().getSize());
                // rough concepts for handling resize
                // set the dimensions based on the frame size
                Dimension frameSize = wrapper.getParent().getParent().getSize();
                int w = (int) Math.ceil(frameSize.getWidth() * .3f);

                userListPanel.setPreferredSize(new Dimension(w, (int) frameSize.getHeight()));
                userListPanel.revalidate();
                userListPanel.repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // System.out.println("Moved to " + e.getComponent().getLocation());
            }
        });
    }

    public void addUserListItem(long clientId, String clientName) {
        userListPanel.addUserListItem(clientId, clientName);
    }

    public void removeUserListItem(long clientId) {
        userListPanel.removeUserListItem(clientId);
    }

    public void clearUserList() {
        userListPanel.clearUserList();
    }
 /*Nm874
 * 12/7/23
 */
    public void addText(String text) {
        JPanel content = chatArea;
        // add message
        
        JEditorPane textContainer = new JEditorPane("text/html", text);

        // sizes the panel to attempt to take up the width of the container
        // and expand in height based on word wrapping
        textContainer.setLayout(null);
        textContainer.setPreferredSize(
                new Dimension(content.getWidth(), ClientUtils.calcHeightForText(this, text, content.getWidth())));
        textContainer.setMaximumSize(textContainer.getPreferredSize());
        textContainer.setEditable(false);
        ClientUtils.clearBackground(textContainer);
        // add to container and tell the layout to revalidate
        content.add(textContainer);
        // scroll down on new message
        JScrollBar vertical = ((JScrollPane) chatArea.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
        logger.log(Level.INFO, "Adding message to history: " + text);
        chatHistory.add(text);
    }
    private void loadChatHistory() {
        for (String message : chatHistory) {
            addTextToPanel(message);
        }
    }
    private void exportChatHistory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Chat History");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Call a method to save the chat history to the selected file
            saveChatToFile(fileToSave, chatHistory);
        }
    }
    
    private void saveChatToFile(File file, ArrayList<String> chatHistory) {
        if (chatHistory.isEmpty()) {
            logger.log(Level.WARNING, "Chat history is empty, nothing to save.");
            return;
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String message : chatHistory) {
                writer.write(message);
                writer.newLine(); // Ensure each message is on a new line
            }
            logger.log(Level.INFO, "Chat history saved successfully to " + file.getAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save chat history", e);
            // Notify the user that saving failed, if your application has a GUI alert/notification system
        }
    }
    private void addTextToPanel(String text) {
        JEditorPane textContainer = new JEditorPane();
        textContainer.setContentType("text/html");
        textContainer.setText(text);
        textContainer.setEditable(false);
        ClientUtils.clearBackground(textContainer);
    
        // Ensure the component sizes itself properly
        textContainer.setSize(chatArea.getWidth(), Short.MAX_VALUE);
        Dimension prefSize = textContainer.getPreferredSize();
        textContainer.setPreferredSize(new Dimension(chatArea.getWidth(), prefSize.height));
        textContainer.setMaximumSize(prefSize);
    
        chatArea.add(textContainer);
    
        // Scroll down to the new message
        chatArea.revalidate();
        chatArea.repaint();
        JScrollBar vertical = ((JScrollPane) chatArea.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
    public void updateUserListItem(long clientId, boolean isMuted) {
        // Code to update the visual representation of the user list item.
        // If 'isMuted' is true, gray out the user's name.
    }
    public String processAllStyles(String text) {
          // Process underline
        text = text.replaceAll("__(.*?)__", "<u>$1</u>");
        // Process bold
        text = text.replaceAll("\\*\\*(.*)\\*\\*", "<b>$1</b>");

        // Process italics
        text = text.replaceAll("_(.*)_", "<i>$1</i>");
    
        // Process colored text
        text = text.replaceAll("\\{color:(#\\w{6})\\}(.*?)\\{color\\}", "<span style='color:$1;'>$2</span>");
    
        return text;
    }
}