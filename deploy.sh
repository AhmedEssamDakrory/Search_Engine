while getopts "cl" option;
do
	case $option in
		c) crawl=true;;
		l) lib=true;;
	esac
done
cd ~/Search_Engine
git pull
javac -cp "lib/*":"lib/mongo-driver-3.6.3/*" src/main/*/*.java
sudo rm -rf /opt/tomcat/webapps/ROOT/WEB-INF/classes/main
sudo cp -r src/main/ /opt/tomcat/webapps/ROOT/WEB-INF/classes
sudo cp -r data/web.xml /opt/tomcat/webapps/ROOT/WEB-INF/
if [ "$lib" == true ]; then
	sudo rm -rf /opt/tomcat/webapps/ROOT/WEB-INF/lib/*
	sudo cp lib/*.jar /opt/tomcat/webapps/ROOT/WEB-INF/lib/
	sudo cp lib/mongo-driver-3.6.3/*.jar /opt/tomcat/webapps/ROOT/WEB-INF/lib/
fi
if [ "$crawl" == true ]; then
	cd /opt/tomcat/webapps/ROOT/WEB-INF/classes
	java -cp .:"../lib/*" main/crawler/WebCrawler
	java -cp .:"../lib/*" main/indexer/Indexer
fi
sudo ufw allow 8080
sudo chown -R tomcat /opt/tomcat/
sudo systemctl restart tomcat
