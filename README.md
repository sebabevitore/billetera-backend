# Billetin API - Backend 🚀

API RESTful desarrollada en Java Spring Boot para **Billetin**, una aplicación de gestión de finanzas personales. Este backend se encarga de procesar la lógica de negocio, gestionar de forma segura la autenticación y calcular los reportes financieros.

## 🛠 Tecnologías y Arquitectura
- **Java 21** & **Spring Boot**
- **Spring Security** (Autenticación Híbrida: JWT propio + Google OAuth2)
- **Spring Data JPA & Hibernate** (Implementación de consultas optimizadas y desactivación de OSIV para mejor rendimiento).
- **MySQL** (Base de datos relacional)
- **Patrón DTO (Data Transfer Object):** Aislamiento de las entidades de la base de datos de las respuestas HTTP.
- **Global Exception Handler:** Intercepción centralizada de errores (400, 403, 404, 500) para respuestas JSON limpias.

## ✨ Características Principales
- **Autenticación Segura:** Login tradicional y validación de tokens de Google Sign-In, emitiendo JWT propios.
- **Gestión Avanzada de Cuentas:** Soporte para cuentas bancarias estándar y **Tarjetas de Crédito**, incluyendo lógica de ciclos de facturación (fechas de cierre y vencimiento) para el cálculo dinámico de saldos.
- **Presupuestos Inteligentes:** Categorías de gastos con soporte para montos límite (`monto_limite`) para el control de presupuestos.
- **Motor de Reportes:** Agregación de datos a nivel base de datos para generar resúmenes de flujos de caja y estadísticas mensuales/anuales.

## ⚙️ Configuración y Ejecución Local

1. Clonar el repositorio:
   ```bash
   git clone [https://github.com/TU-USUARIO/billetera-backend.git](https://github.com/TU-USUARIO/billetera-backend.git)
