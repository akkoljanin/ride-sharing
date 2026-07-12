@echo off
echo ============================================================
echo  Ride-Sharing Microservices - Test Pipeline (Windows)
echo ============================================================

set ROOT=%~dp0
set FAILED=0

echo.
echo [1/7] Testing Util...
cd "%ROOT%Util"
call mvnw.cmd test
if %errorlevel% neq 0 ( echo FAILED: Util & set FAILED=1 )

echo.
echo [2/7] Testing DriverService...
cd "%ROOT%DriverService"
call mvnw.cmd test
if %errorlevel% neq 0 ( echo FAILED: DriverService & set FAILED=1 )

echo.
echo [3/7] Testing PricingService...
cd "%ROOT%PricingService"
call mvnw.cmd test
if %errorlevel% neq 0 ( echo FAILED: PricingService & set FAILED=1 )

echo.
echo [4/7] Testing RideService (unit tests with mocks)...
cd "%ROOT%RideService"
call mvnw.cmd test
if %errorlevel% neq 0 ( echo FAILED: RideService & set FAILED=1 )

echo.
echo [5/7] Testing NotificationService...
cd "%ROOT%NotificationService"
call mvnw.cmd test
if %errorlevel% neq 0 ( echo FAILED: NotificationService & set FAILED=1 )

echo.
echo [6/7] Testing UsersService...
cd "%ROOT%UsersService"
call mvnw.cmd test
if %errorlevel% neq 0 ( echo FAILED: UsersService & set FAILED=1 )

echo.
echo [7/7] Testing NamingServer...
cd "%ROOT%NamingServer"
call mvnw.cmd test
if %errorlevel% neq 0 ( echo FAILED: NamingServer & set FAILED=1 )

echo.
if %FAILED%==1 (
    echo ============================================================
    echo  SOME TESTS FAILED - check output above
    echo ============================================================
    exit /b 1
) else (
    echo ============================================================
    echo  All tests passed!
    echo ============================================================
)
cd "%ROOT%"
