
package main.com.ClienteTelefonia.view;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import main.com.ClienteTelefonia.controller.ClienteTelefoniaController;
import main.com.ClienteTelefonia.model.ClienteTelefonia;
import main.com.ClienteTelefonia.service.CalculoRecargaSimulada;


public class JRecargaTarjeta extends javax.swing.JFrame {

    private ClienteTelefoniaController controller;
    private UUID claveIdempotencia = UUID.randomUUID();
    
    public JRecargaTarjeta(ClienteTelefoniaController controller) {
        initComponents();
        
        this.controller = controller;   
        
        ClienteTelefonia cliente = controller.obtenerClienteActual();
            if(cliente != null) {
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

        lblCliente = new javax.swing.JLabel();
        lblMonto = new javax.swing.JLabel();
        lblTitulo = new javax.swing.JLabel();
        txtCliente = new javax.swing.JTextField();
        txtMonto = new javax.swing.JTextField();
        btnRecarga = new javax.swing.JButton();
        btnVolver = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblCliente.setText("CLIENTE");

        lblMonto.setText("MONTO (PEN)");

        lblTitulo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lblTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitulo.setText("RECARGA CON TARJETA SIMULADA");

        btnRecarga.setText("RECARGAR");
        btnRecarga.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRecargaActionPerformed(evt);
            }
        });

        btnVolver.setText("VOLVER");
        btnVolver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVolverActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(95, 95, 95)
                                .addComponent(btnVolver, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnRecarga, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMonto, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(txtMonto, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(90, 90, 90)
                        .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 105, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 212, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMonto, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtMonto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRecarga, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVolver, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(42, 42, 42))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRecargaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRecargaActionPerformed
        btnRecarga.setEnabled(false);
         try {
                if (!controller.existeCliente()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "No existe un cliente seleccionado."
                    );
                    return;
                }

                BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
                var importes = CalculoRecargaSimulada.tarjeta(monto);

                int confirmar = JOptionPane.showConfirmDialog(
                    this,
                    "SIMULACIÓN ACADÉMICA — no se realiza ningún pago real."
                        + "\nCliente: " + controller.obtenerNombre()
                        + "\nImporte: PEN " + importes.montoBruto()
                        + "\nComisión (2%): PEN " + importes.comision()
                        + "\nImporte acreditado: PEN "
                        + importes.montoAcreditado()
                        + "\n\n¿Registrar la simulación?",
                    "Confirmar recarga simulada",
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

                BigDecimal nuevoSaldo = controller.registrarRecargaTarjeta(
                        monto,
                        claveIdempotencia
                );

                JOptionPane.showMessageDialog(
                    this,
                    "¡RECARGA REGISTRADA CORRECTAMENTE!"
                        + "\nEstado: SIMULADA"
                        + "\nImporte bruto: PEN " + importes.montoBruto()
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
                    "No se pudo registrar la recarga en SQL Server."
                        + "\nDetalle: " + ex.getMessage(),
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE
                    );
                }
        finally {
            btnRecarga.setEnabled(true);
        }
    }//GEN-LAST:event_btnRecargaActionPerformed

    private void btnVolverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVolverActionPerformed
        // BTN VOLVER:
        MenuOpciones menu = new MenuOpciones(controller);
        menu.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnVolverActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnRecarga;
    private javax.swing.JButton btnVolver;
    private javax.swing.JLabel lblCliente;
    private javax.swing.JLabel lblMonto;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextField txtCliente;
    private javax.swing.JTextField txtMonto;
    // End of variables declaration//GEN-END:variables
}
