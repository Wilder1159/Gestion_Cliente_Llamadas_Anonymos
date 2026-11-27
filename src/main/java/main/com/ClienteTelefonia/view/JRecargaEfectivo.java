
package main.com.ClienteTelefonia.view;

import main.com.ClienteTelefonia.controller.ClienteTelefoniaController;
import javax.swing.JOptionPane;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import main.com.ClienteTelefonia.model.ClienteTelefonia;
import main.com.ClienteTelefonia.service.CalculoRecargaSimulada;

public class JRecargaEfectivo extends javax.swing.JFrame {

    private ClienteTelefoniaController controller;
    private UUID claveIdempotencia = UUID.randomUUID();
   
    public JRecargaEfectivo(ClienteTelefoniaController controller) {
        initComponents();
        
        this.controller = controller;
        //obtener el mismo cliente
        ClienteTelefonia cliente = controller.obtenerClienteActual();
        if (cliente != null) {
            txtCliente.setText(cliente.getNombre());
            txtCliente.setEditable(false);
        }
        txtMonto.getDocument().addDocumentListener(new DocumentListener() {
            private void cambioImporte() {
                claveIdempotencia = UUID.randomUUID();
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                cambioImporte();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                cambioImporte();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                cambioImporte();
            }
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblRecarga = new javax.swing.JLabel();
        txtCliente = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtMonto = new javax.swing.JTextField();
        btnGenerarQR = new javax.swing.JButton();
        btnVolver = new javax.swing.JButton();
        btnRecargar = new javax.swing.JButton();
        lblQR = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblRecarga.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lblRecarga.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblRecarga.setText("RECARGA YAPE SIMULADA");
        lblRecarga.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jLabel1.setText("CLIENTE");

        jLabel2.setText("MONTO DE RECARGA (PEN)");

        btnGenerarQR.setText("GENERAR CODIGO QR");
        btnGenerarQR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGenerarQRActionPerformed(evt);
            }
        });

        btnVolver.setText("VOLVER");
        btnVolver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVolverActionPerformed(evt);
            }
        });

        btnRecargar.setText("RECARGAR");
        btnRecargar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRecargarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(lblRecarga, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(txtCliente))
                                .addComponent(txtMonto, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(42, 42, 42)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(btnGenerarQR, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                                    .addComponent(lblQR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(72, 72, 72)
                        .addComponent(btnVolver, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRecargar, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(79, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addComponent(lblRecarga, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtMonto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnGenerarQR)
                .addGap(18, 18, 18)
                .addComponent(lblQR, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnVolver, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRecargar, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(38, 38, 38))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGenerarQRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerarQRActionPerformed
        try {
            BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
            CalculoRecargaSimulada.yape(monto);
            JOptionPane.showMessageDialog(
                    this,
                    "Esta es una simulación académica. "
                    + "No realice ni verifique una transferencia real."
                    + "\nImporte simulado: PEN "
                    + monto.setScale(2, java.math.RoundingMode.HALF_UP)
                            .toPlainString()
            );
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese un importe numérico válido."
            );
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }//GEN-LAST:event_btnGenerarQRActionPerformed

    private void btnVolverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVolverActionPerformed
        // vovler atras
        MenuOpciones menu = new MenuOpciones(controller);
        menu.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnVolverActionPerformed

    private void btnRecargarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRecargarActionPerformed
        btnRecargar.setEnabled(false);
        try {
                if (!controller.existeCliente()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "No existe un cliente seleccionado."
                    );
                    return;
                }

                BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
                var importes = CalculoRecargaSimulada.yape(monto);

                int confirmar = JOptionPane.showConfirmDialog(
                    this,
                    "SIMULACIÓN ACADÉMICA — no se realiza ni verifica "
                        + "ningún pago real."
                        + "\nCliente: " + controller.obtenerNombre()
                        + "\nImporte: PEN " + importes.montoBruto()
                        + "\nComisión: PEN " + importes.comision()
                        + "\nImporte acreditado: PEN "
                        + importes.montoAcreditado()
                        + "\n\n¿Registrar la simulación?",
                    "Confirmar recarga Yape simulada",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                if (confirmar != JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Operación cancelada. No se registró la recarga."
                    );
                    return;
                }

                BigDecimal nuevoSaldo = controller.registrarRecargaYape(
                        monto,
                        claveIdempotencia
                );

                JOptionPane.showMessageDialog(
                    this,
                    "¡RECARGA REGISTRADA CORRECTAMENTE!"
                        + "\nMétodo: Yape simulado"
                        + "\nEstado: SIMULADA"
                        + "\nImporte: PEN " + importes.montoBruto()
                        + "\nComisión: PEN " + importes.comision()
                        + "\nImporte acreditado: PEN "
                        + importes.montoAcreditado()
                        + "\nSaldo actual: PEN " + nuevoSaldo
                );

                MenuOpciones menu = new MenuOpciones(controller);
                menu.setLocationRelativeTo(null);
                menu.setVisible(true);
                this.dispose();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Ingrese un monto numérico válido."
                );
            } catch (IllegalArgumentException | IllegalStateException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "No se pudo registrar la recarga en la base de datos."
                        + "\nDetalle: " + ex.getMessage(),
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        finally {
            btnRecargar.setEnabled(true);
        }
    }//GEN-LAST:event_btnRecargarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGenerarQR;
    private javax.swing.JButton btnRecargar;
    private javax.swing.JButton btnVolver;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblQR;
    private javax.swing.JLabel lblRecarga;
    private javax.swing.JTextField txtCliente;
    private javax.swing.JTextField txtMonto;
    // End of variables declaration//GEN-END:variables
}
