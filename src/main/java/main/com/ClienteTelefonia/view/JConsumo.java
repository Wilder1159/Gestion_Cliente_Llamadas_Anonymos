        
package main.com.ClienteTelefonia.view;

import main.com.ClienteTelefonia.controller.ClienteTelefoniaController;
import javax.swing.JOptionPane;
import main.com.ClienteTelefonia.service.LlamadaApiClient;
import java.util.List;
import java.util.UUID;

public class JConsumo extends javax.swing.JFrame {

    private ClienteTelefoniaController controller;
    private boolean llamadaActiva = false;
    private UUID claveIdempotenciaPendiente;
    private String destinoPendiente;
    
    
    public JConsumo(ClienteTelefoniaController controller) {
        initComponents();
        this.controller = controller;
        
        setLocationRelativeTo(null);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        
        if(controller.obtenerClienteActual() != null) {
            txtCliente.setText(controller.obtenerNombre());
        
            txtSaldo.setText(String.format("%.2f", controller.obtenerSaldoActual()));
        }
        
        txtCliente.setEditable(false);
        txtSaldo.setEditable(false);
        txtMonto.setEditable(false);
        
        txtMonto.setText("-");
        lblCronometro.setText(
                "<html><center>Las llamadas pueden continuar "
                + "simultáneamente en el servidor.</center></html>"
        );
        lblCosto.setText("Costo final (PEN):");
    }
    

   
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        lblCliente = new javax.swing.JLabel();
        lblSaldo = new javax.swing.JLabel();
        txtCliente = new javax.swing.JTextField();
        txtSaldo = new javax.swing.JTextField();
        btnConsumir = new javax.swing.JButton();
        btnVolver = new javax.swing.JButton();
        btnFinalizar = new javax.swing.JButton();
        lblCosto = new javax.swing.JLabel();
        txtMonto = new javax.swing.JTextField();
        lblCronometro = new javax.swing.JLabel();
        lblNumeroDestino = new javax.swing.JLabel();
        txtNumeroDestino = new javax.swing.JTextField();
        lblEstadoLlamada = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitulo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lblTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitulo.setText("LLAMADA SALIENTE");

        lblCliente.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        lblCliente.setText("CLIENTE");

        lblSaldo.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        lblSaldo.setText("SALDO DISPONIBLE");

        txtCliente.setEditable(false);

        btnConsumir.setText("LLAMAR");
        btnConsumir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConsumirActionPerformed(evt);
            }
        });

        btnVolver.setText("VOLVER");
        btnVolver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVolverActionPerformed(evt);
            }
        });

        btnFinalizar.setText("VER / FINALIZAR");
        btnFinalizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFinalizarActionPerformed(evt);
            }
        });

        lblCosto.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        lblCosto.setText("COSTO ESTIMADO");

        lblNumeroDestino.setText("NUMERO DE DESTINO");

        lblEstadoLlamada.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblEstadoLlamada.setText("Estado: En espera");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblCronometro, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addComponent(lblEstadoLlamada, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(33, 33, 33)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblCosto, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMonto, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblNumeroDestino)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtNumeroDestino, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(55, 55, 55))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblSaldo, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                                .addGap(18, 18, 18))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(2, 2, 2)
                                .addComponent(lblCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtSaldo, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(37, 37, 37))))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(btnVolver, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnFinalizar, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnConsumir, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(76, 76, 76)
                        .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSaldo, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtSaldo, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNumeroDestino, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtNumeroDestino, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblCronometro, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblEstadoLlamada, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMonto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCosto))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnFinalizar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnConsumir, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVolver, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnVolverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVolverActionPerformed
        if (llamadaActiva) {
            JOptionPane.showMessageDialog(
                    this, "Espere a que se registre la solicitud."
            );
            return;
        }
        
        MenuOpciones menu = new MenuOpciones(controller);
        menu.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnVolverActionPerformed

    private void btnConsumirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConsumirActionPerformed
        if (llamadaActiva) {
            return;
        }
        iniciarLlamadaReal();

    }//GEN-LAST:event_btnConsumirActionPerformed

    private void btnFinalizarActionPerformed(
            java.awt.event.ActionEvent evt
    ) {
        if (llamadaActiva) {
            JOptionPane.showMessageDialog(
                    this, "Espere a que termine el envío de la solicitud."
            );
            return;
        }
        btnFinalizar.setEnabled(false);
        new javax.swing.SwingWorker<List<LlamadaApiClient.LlamadaVista>, Void>() {
            @Override
            protected List<LlamadaApiClient.LlamadaVista> doInBackground()
                    throws Exception {
                return crearApiClient().listar();
            }

            @Override
            protected void done() {
                btnFinalizar.setEnabled(true);
                try {
                    List<LlamadaApiClient.LlamadaVista> llamadas = get();
                    if (llamadas.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                JConsumo.this,
                                "Todavía no hay llamadas registradas."
                        );
                        return;
                    }
                    Object seleccion = JOptionPane.showInputDialog(
                            JConsumo.this,
                            "Historial de llamadas (elige una para finalizar):",
                            "Llamadas",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            llamadas.toArray(),
                            llamadas.get(0)
                    );
                    if (seleccion instanceof LlamadaApiClient.LlamadaVista llamada) {
                        if (esTerminal(llamada.estado())) {
                            JOptionPane.showMessageDialog(
                                    JConsumo.this,
                                    "Estado: " + llamada.estado() + "\n"
                                    + "Duración facturable: "
                                    + llamada.duracionSegundos() + " s\n"
                                    + "Costo: " + llamada.moneda() + " "
                                    + llamada.costo().toPlainString()
                                    + (llamada.motivoFinalizacion() == null
                                            ? ""
                                            : "\nMotivo: "
                                                + llamada.motivoFinalizacion())
                            );
                        } else {
                            confirmarFinalizacion(llamada);
                        }
                    }
                } catch (Exception ex) {
                    mostrarErrorApi("No se pudo consultar el historial", ex);
                }
            }
        }.execute();
    }

    private void iniciarLlamadaReal() {
        if (!controller.existeCliente()) {
            JOptionPane.showMessageDialog(
                    this, "Primero debe registrar un cliente."
            );
            return;
        }
        String destino = txtNumeroDestino.getText()
                .trim()
                .replaceAll("[\\s-]", "");
        if (destino.matches("9\\d{8}")) {
            destino = "+51" + destino;
        }
        if (!destino.matches("^\\+[1-9]\\d{7,14}$")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese un número válido en formato internacional.\n"
                    + "Ejemplo peruano: +519XXXXXXXX"
            );
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Desea solicitar la llamada a " + destino + "?\n"
                + "La comunicación y los cargos de Telnyx son reales.",
                "Confirmar llamada",
                JOptionPane.YES_NO_OPTION
        );
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        llamadaActiva = true;
        btnConsumir.setEnabled(false);
        btnFinalizar.setEnabled(false);
        lblEstadoLlamada.setText("Estado: Registrando solicitud...");
        final String numeroDestino = destino;
        if (!destino.equals(destinoPendiente)
                || claveIdempotenciaPendiente == null) {
            destinoPendiente = destino;
            claveIdempotenciaPendiente = UUID.randomUUID();
        }
        final UUID claveIdempotencia = claveIdempotenciaPendiente;

        new javax.swing.SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws Exception {
                return crearApiClient().iniciar(
                        numeroDestino, claveIdempotencia
                );
            }

            @Override
            protected void done() {
                try {
                    long idLlamada = get();
                    claveIdempotenciaPendiente = null;
                    destinoPendiente = null;
                    lblEstadoLlamada.setText(
                            "Solicitud registrada: #" + idLlamada
                    );
                    JOptionPane.showMessageDialog(
                            JConsumo.this,
                            "Solicitud #" + idLlamada + " registrada.\n"
                            + "El estado y costo final se consultan "
                            + "en «VER / FINALIZAR»."
                    );
                    txtNumeroDestino.setText("");
                } catch (Exception ex) {
                    lblEstadoLlamada.setText(
                            "Estado: No se pudo registrar"
                    );
                    mostrarErrorApi(
                            "No se pudo registrar la llamada", ex
                    );
                } finally {
                    llamadaActiva = false;
                    btnConsumir.setEnabled(true);
                    btnFinalizar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void confirmarFinalizacion(
            LlamadaApiClient.LlamadaVista llamada
    ) {
        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Solicitar a Telnyx finalizar la llamada #"
                + llamada.idLlamada() + " a " + llamada.destino() + "?",
                "Finalizar llamada",
                JOptionPane.YES_NO_OPTION
        );
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }
        btnFinalizar.setEnabled(false);
        new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                crearApiClient().finalizar(llamada.idLlamada());
                return null;
            }

            @Override
            protected void done() {
                btnFinalizar.setEnabled(true);
                try {
                    get();
                    lblEstadoLlamada.setText(
                            "Estado: Finalización solicitada"
                    );
                    JOptionPane.showMessageDialog(
                            JConsumo.this,
                            "Solicitud de finalización enviada. "
                            + "El cierre y el cargo se confirmarán "
                            + "al recibir el webhook."
                    );
                } catch (Exception ex) {
                    mostrarErrorApi(
                            "No se pudo solicitar la finalización", ex
                    );
                }
            }
        }.execute();
    }

    private LlamadaApiClient crearApiClient() {
        return new LlamadaApiClient(controller.obtenerTokenSesion());
    }

    private boolean esTerminal(String estado) {
        return "FINALIZADA".equals(estado)
                || "FALLIDA".equals(estado)
                || "CANCELADA".equals(estado);
    }

    private void mostrarErrorApi(String titulo, Exception error) {
        Throwable causa = error;
        while (causa.getCause() != null
                && !(causa instanceof java.io.IOException)) {
            causa = causa.getCause();
        }
        JOptionPane.showMessageDialog(
                this,
                titulo + ".\n" + causa.getMessage()
                + "\nVerifique que el servidor de llamadas esté activo.",
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConsumir;
    private javax.swing.JButton btnFinalizar;
    private javax.swing.JButton btnVolver;
    private javax.swing.JLabel lblCliente;
    private javax.swing.JLabel lblCosto;
    private javax.swing.JLabel lblCronometro;
    private javax.swing.JLabel lblEstadoLlamada;
    private javax.swing.JLabel lblNumeroDestino;
    private javax.swing.JLabel lblSaldo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextField txtCliente;
    private javax.swing.JTextField txtMonto;
    private javax.swing.JTextField txtNumeroDestino;
    private javax.swing.JTextField txtSaldo;
    // End of variables declaration//GEN-END:variables

 
}
