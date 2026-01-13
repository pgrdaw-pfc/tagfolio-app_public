# Tagfolio

**Autor:** Pablo Gimeno  
**Email:** pgrdaw@gmail.com

## Descripción
Tagfolio es una aplicación para la gestión de imágenes, filtros y generación de informes.  
Permite clasificar y etiquetar imágenes, crear filtros complejos y generar informes PDF.  
Los recursos pueden compartirse mediante URLs públicas, y el acceso se controla mediante roles (ADMIN, USER, ANONYMOUS).

## Requisitos
- Servidor Linux con privilegios de administrador (sudo) 
- Git
- Conexión a internet
- Archivo `.env` configurado (ver `.env.example`)

## Despliegue local y entorno de pruebas (TEST)

Para desplegar Tagfolio en un entorno de pruebas local, se utiliza el script `deploy.sh`:

### Uso
1. Clonar el repositorio:
```bash
git clone https://github.com/pgrdaw-pfc/tagfolio-app_public.git
cd tagfolio-app_public
```
2. Comprobar que Docker está instalado y que el usuario tiene permisos:
```bash
docker info
```
3. Ejecutar el despliegue TEST:
```bash
./deploy.sh
```
Este script realiza:
- Comprobación de Docker y permisos  
- Detención de servicios existentes (`docker compose down`)  
- Pull de cambios recientes desde Git  
- Construcción de imágenes y arranque de contenedores (`docker compose up --build -d`)  
- Limpieza de imágenes antiguas (`docker image prune -f`)

### Acceso
Por defecto, la aplicación se levantará en `http://localhost`.

## Despliegue en producción (AWS)

Para desplegar en producción se utiliza el script `deploy.aws.sh`:

### Configuración previa
- Claves SSH de las instancias EC2  
- IP pública de la aplicación y de la base de datos  
- Variables de entorno en `.env` (DB_USER, DB_PASSWORD)

### Uso
```bash
./deploy.aws.sh
```
El script realiza:
- Copia de contextos Docker a las instancias EC2  
- Arranque del contenedor Oracle en la instancia de base de datos  
- Arranque de la aplicación Spring Boot en la instancia de app  
- Verificación mediante healthcheck  
- Limpieza de imágenes antiguas

### Observaciones
- No se copia almacenamiento local, los datos se preservan en volumenes persistentes  
- Se recomienda ejecutar desde la rama `main`

## Generación de documentación interna

Se puede generar toda la documentación automáticamente con `generate-docs.sh`:

### Uso
```bash
./generate-docs.sh
```
Genera:
- Javadoc (Java)  
- JSDoc (JavaScript)  
- KSS (CSS)

Toda la documentación se genera en `build/docs` y se entrega también en un ZIP anexo.

## Contacto
**Autor:** Pablo Gimeno  
**Email:** pgrdaw@gmail.com

## Scripts incluidos

- `deploy.sh`: despliegue local / TEST  
- `deploy.aws.sh`: despliegue en producción AWS  
- `create-admin.aws.sh`: generación del primer ADMIN en AWS  
- `generate-docs.sh`: generación automática de documentación
