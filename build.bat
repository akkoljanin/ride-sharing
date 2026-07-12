@echo off
echo ============================================================
echo  Ride-Sharing Microservices - Build Pipeline (Windows)
echo ============================================================

set ROOT=%~dp0

echo.
echo [1/9] Building Util...
cd "%ROOT%Util"
call mvnw.cmd clean install -DskipTests
if %errorlevel% neq 0 ( echo FAILED: Util & exit /b 1 )

echo.
echo [2/9] Building ServiceLibrary...
cd "%ROOT%ServiceLibrary"
call mvnw.cmd clean install -DskipTests
if %errorlevel% neq 0 ( echo FAILED: ServiceLibrary & exit /b 1 )

echo.
echo [3/9] Building NamingServer...
cd "%ROOT%NamingServer"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 ( echo FAILED: NamingServer & exit /b 1 )
docker build -t ridesharing/naming-server:1.0.0 .

echo.
echo [4/9] Building UsersService...
cd "%ROOT%UsersService"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 ( echo FAILED: UsersService & exit /b 1 )
docker build -t ridesharing/users-service:1.0.0 .

echo.
echo [5/9] Building DriverService...
cd "%ROOT%DriverService"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 ( echo FAILED: DriverService & exit /b 1 )
docker build -t ridesharing/driver-service:1.0.0 .

echo.
echo [6/9] Building PricingService...
cd "%ROOT%PricingService"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 ( echo FAILED: PricingService & exit /b 1 )
docker build -t ridesharing/pricing-service:1.0.0 .

echo.
echo [7/9] Building RideService...
cd "%ROOT%RideService"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 ( echo FAILED: RideService & exit /b 1 )
docker build -t ridesharing/ride-service:1.0.0 .

echo.
echo [8/9] Building NotificationService...
cd "%ROOT%NotificationService"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 ( echo FAILED: NotificationService & exit /b 1 )
docker build -t ridesharing/notification-service:1.0.0 .

echo.
echo [9/9] Building APIGateway...
cd "%ROOT%APIGateway"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 ( echo FAILED: APIGateway & exit /b 1 )
docker build -t ridesharing/api-gateway:1.0.0 .

echo.
echo ============================================================
echo  Build complete! Run:  docker-compose up
echo ============================================================
cd "%ROOT%"
