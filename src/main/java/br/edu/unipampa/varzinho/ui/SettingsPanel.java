package br.edu.unipampa.varzinho.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

/** Operator-controlled visual preferences that apply immediately to every window. */
final class SettingsPanel extends JPanel {
    SettingsPanel(CourtPanel courts, java.util.function.Consumer<String> messages) {
        super(new MigLayout("insets 24, fillx, wrap 1", "[grow,fill]", "[]8[]24[]"));
        VarZinhoTheme.styleCard(this);
        JLabel title = new JLabel("Aparência");
        VarZinhoTheme.styleTitle(title);
        add(title);
        add(new JLabel("Escolha o tema mais confortável para operar durante a partida."));
        javax.swing.JButton theme = SidebarPanel.buildThemeButton();
        VarZinhoTheme.styleButton(theme, ButtonRole.SECONDARY);
        add(theme, "growx 0, " + VarZinhoTheme.CONTROL_SIZE);
        JLabel identity = new JLabel("Identificação da quadra");
        VarZinhoTheme.styleTitle(identity);
        add(identity, "gaptop 18");
        JLabel label = new JLabel();
        javax.swing.JTextField name = new javax.swing.JTextField();
        label.setLabelFor(name);
        Runnable refresh = () -> {
            int number = courts.selectedCourtNumber();
            label.setText("Nome de exibição da quadra " + number);
            name.setText(courts.nameOf(number));
        };
        courts.onCourtChange(refresh);
        refresh.run();
        add(label);
        add(name, "growx 0, w 360!, " + VarZinhoTheme.CONTROL_SIZE);
        javax.swing.JButton apply = new javax.swing.JButton("Salvar nome");
        apply.addActionListener(event -> courts.renameSelected(apply, name.getText(), () -> {
            refresh.run();
            messages.accept("Nome de exibição atualizado.");
        }));
        EnterKey.submits(apply, name);
        VarZinhoTheme.styleButton(apply, ButtonRole.PRIMARY);
        add(apply, "growx 0, " + VarZinhoTheme.CONTROL_SIZE);
        JLabel capture = new JLabel("Gravação com celular");
        VarZinhoTheme.styleTitle(capture);
        add(capture, "gaptop 18");
        javax.swing.JButton guide = new javax.swing.JButton("Como conectar a câmera");
        guide.addActionListener(event -> showConnectionGuide());
        VarZinhoTheme.styleButton(guide, ButtonRole.SECONDARY);
        add(guide, "growx 0, " + VarZinhoTheme.CONTROL_SIZE);
    }

    private void showConnectionGuide() {
        javax.swing.JTextArea guide = new javax.swing.JTextArea("""
                1. Conecte o celular e o computador à mesma rede Wi-Fi.
                2. No Android, abra o IP Webcam e inicie o servidor.
                3. No computador, confira a transmissão com o FFplay. Troque o endereço de exemplo pelo endereço de vídeo do celular.

                ffplay http://192.168.0.42:8080/video

                4. Feche a prévia e o VarZinho. No PowerShell, dentro da pasta do projeto, execute:

                $env:VARZINHO_STREAM_URL = "http://192.168.0.42:8080/video"
                mvn compile exec:java

                5. Abra a quadra da câmera phone-1. Quando o buffer chegar a 30 segundos, clique em Salvar Melhor Momento. Em Arquivos, selecione o lance e clique em Assistir ao vídeo.

                Se o comando ffplay não for reconhecido, instale o FFmpeg com winget install Gyan.FFmpeg e abra um novo PowerShell.""", 16, 45);
        guide.setEditable(false);
        guide.setLineWrap(true);
        guide.setWrapStyleWord(true);
        guide.setCaretPosition(0);
        guide.getAccessibleContext().setAccessibleName("Passos para gravar com o celular");
        javax.swing.JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(guide),
                "Conectar câmera pelo Wi-Fi", javax.swing.JOptionPane.PLAIN_MESSAGE);
    }
}
