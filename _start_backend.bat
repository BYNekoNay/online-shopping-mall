@echo off
cd /d d:\codegitee\online-shopping-mall\backend
set DB_PASSWORD=123456
mvn.cmd -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev > d:\codegitee\online-shopping-mall\_tmp_run3.log 2>&1
