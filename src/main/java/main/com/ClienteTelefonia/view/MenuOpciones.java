
package main.com.ClienteTelefonia.view;

import main.com.ClienteTelefonia.controller.ClienteTelefoniaController;
import main.com.ClienteTelefonia.controller.LoginController;
import main.com.ClienteTelefonia.view.JRecarga;


public class MenuOpciones extends javax.swing.JFrame {
    
    //ATRIBUTOS
    private ClienteTelefoniaController controller;
    private String rolUsuario;

   //se agrega en el constructor defecto y otro para el parametro de volver
    public MenuOpciones() {
        initComponents();
        
        controller = new ClienteTelefoniaController();
        this.rolUsuario = "CLIENTE";
        configurarMenuPorRol();
     }
    
        //constructos con parametros del controlador
        public MenuOpciones(ClienteTelefoniaController controller){
        initComponents();
        this.controller = controller;
        this.rolUsuario = "CLIENTE";
        configurarMenuPorRol();
        }
        
        //3ER CONSTRUCTOR, EL ROL Y CONTROLADOR PARA LAS VALIDACIONES DE MOSTRAR OPCIONES OCULTAS
        public MenuOpciones(ClienteTelefoniaController controller, String rolUsuario) {
            initComponents();
            this.controller = controller;
            this.rolUsuario = rolUsuario;
            configurarMenuPorRol();
            
        }
        
        
        //nuevo metodo para validar el rol
        private void configurarMenuPorRol(){
        boolean esAdmin = "ADMIN".equalsIgnoreCase(rolUsuario);
      
        //SOLO EL ADMIN PUEDE GESTIONAR GESTIONAR CLIENTES
        cbmoGestionClientes.setVisible(esAdmin);
    }

  
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblCom1 = new javax.swing.JLabel();
        lblCom2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        scrolPanel = new javax.swing.JScrollPane();
        lblMenuPrincipal = new javax.swing.JMenuBar();
        menuInicio = new javax.swing.JMenu();
        itemSalir = new javax.swing.JMenuItem();
        menuHistorial = new javax.swing.JMenu();
        itemReporte = new javax.swing.JMenuItem();
        cbmoGestionClientes = new javax.swing.JMenuItem();
        menuOperaciones = new javax.swing.JMenu();
        itemConsumo = new javax.swing.JMenuItem();
        menuRecarga = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAutoRequestFocus(false);
        setBackground(new java.awt.Color(0, 0, 51));

        lblCom1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lblCom1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCom1.setText("Welcome to the customer registration software");

        lblCom2.setText("Developers: SmithDev and Emanuel");

        jButton1.setText("jButton1");

        jButton2.setText("jButton2");

        lblMenuPrincipal.setAlignmentX(1.0F);
        lblMenuPrincipal.setAlignmentY(1.0F);
        lblMenuPrincipal.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        menuInicio.setText("Inicio");

        itemSalir.setText("Cerrar Sesion");
        itemSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemSalirActionPerformed(evt);
            }
        });
        menuInicio.add(itemSalir);

        lblMenuPrincipal.add(menuInicio);

        menuHistorial.setText("Historial");

        itemReporte.setText("Reporte");
        itemReporte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemReporteActionPerformed(evt);
            }
        });
        menuHistorial.add(itemReporte);

        cbmoGestionClientes.setText("Gestion Clientes");
        cbmoGestionClientes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbmoGestionClientesActionPerformed(evt);
            }
        });
        menuHistorial.add(cbmoGestionClientes);

        lblMenuPrincipal.add(menuHistorial);

        menuOperaciones.setText("Operaciones");

        itemConsumo.setText("Llamar");
        itemConsumo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemConsumoActionPerformed(evt);
            }
        });
        menuOperaciones.add(itemConsumo);

        menuRecarga.setText("Recarga");
        menuRecarga.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRecargaActionPerformed(evt);
            }
        });
        menuOperaciones.add(menuRecarga);

        lblMenuPrincipal.add(menuOperaciones);

        setJMenuBar(lblMenuPrincipal);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrolPanel)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(186, 186, 186)
                        .addComponent(lblCom1, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(224, 224, 224)
                        .addComponent(lblCom2)))
                .addContainerGap(223, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrolPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 518, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblCom1, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblCom2)
                .addGap(12, 12, 12))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void itemSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemSalirActionPerformed
        //para salir del programa en el btn de cerra sesion 
        try {
            if (controller != null) {
                controller.cerrarSesion();
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se pudo cerrar la sesión en el servidor.\n"
                    + e.getMessage(),
                    "Error de sesión",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        LoginController loginController = new LoginController();
        
        JLogin login = new JLogin(loginController);
        login.setVisible(true);
        
        
    }//GEN-LAST:event_itemSalirActionPerformed

    private void itemConsumoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemConsumoActionPerformed
        // crear el boton de consumo desde el menu de opciones
        if (!controller.existeCliente()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Crear primero al cliente para poder realizar la operacion.");
            return;
        } 
        //esta ob se trata al momento de dar click al consumo, nos da a la ventana de consumo
        JConsumo viewConsumo = new JConsumo(controller);
        viewConsumo.setVisible(false);
        
    }//GEN-LAST:event_itemConsumoActionPerformed

    private void itemReporteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemReporteActionPerformed
        // para abrir el JReporte
        if (!controller.existeCliente()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Registra a tu cliente y realizar el consumo y recarga para poder mostrar la informacion.");
        return;
        }
        JReporte reporte = new JReporte(controller);
        reporte.setVisible(true);
        
    }//GEN-LAST:event_itemReporteActionPerformed

    private void menuRecargaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRecargaActionPerformed
        // se ingresa al controlador para abrir la opcion y abrir el view recarga
        if (!controller.existeCliente()) {
            javax.swing.JOptionPane.showMessageDialog(this, "PRIMERO DEBES DE CREAR AL CLIENTE.");
            return;
        }
        
        //aca ingresa el objeto para abrir la ventana
        JRecarga viewRecarga = new JRecarga(controller);
        viewRecarga.setVisible(true);
        
    }//GEN-LAST:event_menuRecargaActionPerformed

    private void cbmoGestionClientesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbmoGestionClientesActionPerformed
        //AQUI SE LLAMA EL OBJETO
        JGestionClientes viewGestion = new JGestionClientes(controller);
        viewGestion.setVisible(true);
        
    }//GEN-LAST:event_cbmoGestionClientesActionPerformed
 

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem cbmoGestionClientes;
    private javax.swing.JMenuItem itemConsumo;
    private javax.swing.JMenuItem itemReporte;
    private javax.swing.JMenuItem itemSalir;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel lblCom1;
    private javax.swing.JLabel lblCom2;
    private javax.swing.JMenuBar lblMenuPrincipal;
    private javax.swing.JMenu menuHistorial;
    private javax.swing.JMenu menuInicio;
    private javax.swing.JMenu menuOperaciones;
    private javax.swing.JMenuItem menuRecarga;
    private javax.swing.JScrollPane scrolPanel;
    // End of variables declaration//GEN-END:variables
}
