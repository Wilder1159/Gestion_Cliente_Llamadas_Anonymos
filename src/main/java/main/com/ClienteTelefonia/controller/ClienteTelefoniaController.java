
package main.com.ClienteTelefonia.controller;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

import main.com.ClienteTelefonia.dao.ClienteDAO;
import main.com.ClienteTelefonia.model.ClienteTelefonia;
import main.com.ClienteTelefonia.server.security.AuthSessionService;
import main.com.ClienteTelefonia.server.security.Rol;

public class ClienteTelefoniaController {

    private ClienteTelefonia clienteActual;
    private final ClienteDAO clienteDAO;
    private final long idClienteAutenticado;
    private final String rolAutenticado;
    private volatile String tokenSesion;

    public ClienteTelefoniaController() {
        this.clienteActual = null;
        this.clienteDAO = new ClienteDAO();
        this.idClienteAutenticado = 0;
        this.rolAutenticado = null;
    }

    public ClienteTelefoniaController(
            long idClienteAutenticado,
            String rolAutenticado
    ) {
        if (idClienteAutenticado <= 0) {
            throw new IllegalArgumentException(
                    "El usuario autenticado no tiene un cliente válido."
            );
        }

        Rol.desde(rolAutenticado);
        this.clienteActual = null;
        this.clienteDAO = new ClienteDAO();
        this.idClienteAutenticado = idClienteAutenticado;
        this.rolAutenticado = rolAutenticado;
    }

    // SE ACTUALIZ PARA crea un cliente solo en memoria.
    // Se conserva para no romper la demas ventanas que lo utilizan.
    public void crearCliente(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre es obligatorio."
            );
        }

        this.clienteActual = new ClienteTelefonia(nombre);
    }

    // Nuevo método: registra el cliente en SQL Server.
    public ClienteTelefonia registrarCliente(
            String nombre,
            String apellido,
            String telefono,
            String correo) throws SQLException {

        validarTexto(nombre, "El nombre es obligatorio.");
        validarTexto(apellido, "El apellido es obligatorio.");
        validarTexto(telefono, "El teléfono es obligatorio.");

        nombre = nombre.trim();
        apellido = apellido.trim();
        telefono = telefono.trim();

        if (nombre.length() > 160) {
            throw new IllegalArgumentException(
                "El nombre no puede superar los 160 caracteres."
            );
        }

        if (apellido.length() > 160) {
            throw new IllegalArgumentException(
                "El apellido no puede superar los 160 caracteres."
            );
        }

        if (telefono.length() > 16) {
            throw new IllegalArgumentException(
                "El teléfono no puede superar los 16 caracteres."
            );
        }

        if (correo != null && correo.isBlank()) {
            correo = null;
        } else if (correo != null) {
            correo = correo.trim();

            if (correo.length() > 300) {
                throw new IllegalArgumentException(
                    "El correo no puede superar los 300 caracteres."
                );
            }
        }

        ClienteTelefonia nuevoCliente =
            new ClienteTelefonia(
                nombre,
                apellido,
                telefono,
                correo
            );

        // Solo se establece como cliente actual
        // después de que SQL Server confirme el registro.
        clienteDAO.registrar(nuevoCliente);

        this.clienteActual = nuevoCliente;

        return nuevoCliente;
    }

    private void validarTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }
    
    //metodo para verificar si exite el cliente
    public ClienteTelefonia obtenerClienteActual() {
        return clienteActual;
    }

    public boolean existeCliente() {
        return clienteActual != null;
    }

    public void establecerTokenSesion(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "El token de sesión no puede estar vacío."
            );
        }
        tokenSesion = token;
    }

    public String obtenerTokenSesion() {
        return tokenSesion;
    }

    public void cerrarSesion() throws SQLException {
        String token = tokenSesion;
        if (token != null) {
            new AuthSessionService().cerrarSesion(token);
            tokenSesion = null;
        }
    }
    
    //metodo para realizar recarga, si exite el cliente
    public void realizarRecarga(int tipo, double monto) {
        if (!existeCliente()) {
            throw new IllegalStateException(
                "Primero debe registrar un cliente."
            );
        }

        clienteActual.realizarRecarga(tipo, monto);
    }

    public boolean realizarConsumo(double monto) {
        if (!existeCliente()) {
            throw new IllegalStateException(
                "Primero debe registrar un cliente."
            );
        }

        return clienteActual.realizarConsumo(monto);
    }

    //nuevo metodo para eliminar al cliente, sin perder el historial de consumo y recarga
    //se usa en gestion y no se cambia, porque, tuve más de 50 errores en compilacion
    public java.util.List<ClienteTelefonia> listarClientes()
        throws java.sql.SQLException {

        return clienteDAO.listar();
    }
    
       //nuevo metodo: eliminar al cliente con el controllador llamando al DAO
       public boolean eliminarCliente(long idCliente) throws SQLException {
        return clienteDAO.eliminar(idCliente);
    }

    
    //metodo nuevo: Cliente exite en SQL y se recupera desde JGestion para operaciones 
    public void seleccionarCliente(long idCliente) throws SQLException {
        if (idCliente <= 0) {
            throw new IllegalArgumentException(
                "El ID del cliente no es válido."
            );
        }

        if (Rol.CLIENTE.name().equalsIgnoreCase(rolAutenticado)
                && idCliente != idClienteAutenticado) {
            throw new IllegalArgumentException(
                    "Un cliente solo puede seleccionar su propia cuenta."
            );
        }

        ClienteTelefonia clienteSeleccionado =
            clienteDAO.buscarPorId(idCliente);

        if (clienteSeleccionado == null) {
            throw new IllegalArgumentException(
                "No se encontró el cliente seleccionado."
            );
        }

        if (!"ACTIVO".equalsIgnoreCase(clienteSeleccionado.getEstado())) {
            throw new IllegalStateException(
                "No se puede seleccionar un cliente inactivo."
                    
            );
        }

        this.clienteActual = clienteSeleccionado;
    }


    //metodo nuevo: para registrrar recargas por YAPE
    public double registrarRecargaYape(double monto) throws SQLException {
        if (!Double.isFinite(monto) || monto <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor que cero."
            );
        }
        return registrarRecargaYape(
                BigDecimal.valueOf(monto),
                UUID.randomUUID()
        ).doubleValue();
    }

    public BigDecimal registrarRecargaYape(
            BigDecimal monto,
            UUID claveIdempotencia
    ) throws SQLException {
        validarAccesoRecarga();

        BigDecimal nuevoSaldo = clienteDAO.registrarRecargaYape(
                clienteActual.getIdCliente(),
                monto,
                claveIdempotencia
        );

        clienteActual.setSaldoFinal(nuevoSaldo.doubleValue());
        clienteActual.setRecargasEfectivo(
                clienteActual.getRecargasEfectivo() + 1
        );

        return nuevoSaldo;
    }

    //metodo nuevo: para registrar recargas por tarjeta
    public double registrarRecargaTarjeta(double monto) throws SQLException {
        if (!Double.isFinite(monto) || monto <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor que cero."
            );
        }
        return registrarRecargaTarjeta(
                BigDecimal.valueOf(monto),
                UUID.randomUUID()
        ).doubleValue();
    }

    public BigDecimal registrarRecargaTarjeta(
            BigDecimal monto,
            UUID claveIdempotencia
    ) throws SQLException {
        validarAccesoRecarga();

        BigDecimal nuevoSaldo = clienteDAO.registrarRecargaTarjeta(
                clienteActual.getIdCliente(),
                monto,
                claveIdempotencia
        );

        clienteActual.setSaldoFinal(nuevoSaldo.doubleValue());
        clienteActual.setRecargasTarjeta(
                clienteActual.getRecargasTarjeta() + 1
        );

        return nuevoSaldo;
    }

    private void validarAccesoRecarga() {
        if (!existeCliente()) {
            throw new IllegalStateException(
                "Primero debe seleccionar un cliente."
            );
        }
        if (idClienteAutenticado <= 0 || rolAutenticado == null) {
            throw new IllegalStateException(
                    "Se requiere una sesión autenticada para recargar."
            );
        }

        Rol rol = Rol.desde(rolAutenticado);
        if (rol == Rol.CLIENTE
                && clienteActual.getIdCliente() != idClienteAutenticado) {
            throw new IllegalStateException(
                    "Un cliente solo puede recargar su propia cuenta."
            );
        }
    }




    //metodos de obtener los datos del cliente para mostrarlos en el reporte
    public String obtenerNombre() {
        return clienteActual.getNombre();
    }

    public Double obtenerSaldoActual() {
        return clienteActual.getSaldoFinal();
    }

    public int obtenerRecargasTarjeta() {
        return clienteActual.getRecargasTarjeta();
    }

    public int obtenerRecargasEfectivo() {
        return clienteActual.getRecargasEfectivo();
    }

    public int obtenerTotalConsumo() {
        return clienteActual.getTotalConsumos();
    }
}
