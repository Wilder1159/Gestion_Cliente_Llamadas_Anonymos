
package main.com.ClienteTelefonia.server.security;

/**
 * Catálogo centralizado de permisos del sistema.
 *
 * Cada permiso representa una operación concreta
 * que podrá autorizarse mediante la política RBAC.
 */
public enum Permiso {

    // SESIONES
    SESION_VER_PROPIA,
    SESION_CERRAR_PROPIA,

    // ==============================
    // CLIENTES
    CLIENTE_VER_PROPIO,
    CLIENTE_LISTAR_TODOS,
    CLIENTE_CREAR,
    CLIENTE_ACTUALIZAR_PROPIO,
    CLIENTE_ACTUALIZAR_TODOS,
    CLIENTE_DESACTIVAR,

    // ==============================
    // SALDOS
    SALDO_VER_PROPIO,
    SALDO_VER_TODOS,

    // ==============================
    // RECARGAS
    RECARGA_CREAR_PROPIA,
    RECARGA_VER_HISTORIAL_PROPIO,
    RECARGA_VER_HISTORIAL_TODOS,

    // ==============================
    // LLAMADAS
    LLAMADA_INICIAR_PROPIA,
    LLAMADA_VER_REGISTROS_PROPIOS,
    LLAMADA_VER_TODAS,

    // ==============================
    // REPORTES
    REPORTE_VER_GENERAL
}
