up:
	sudo docker-compose --env-file ./deploy.env up --build -d

hard-rm:
	sudo docker-compose --env-file ./deploy.env stop && \
	sudo docker-compose --env-file ./deploy.env rm -f && \
    sudo docker volume rm segmentation_app_postgres_data

soft-rm:
	sudo docker-compose --env-file ./deploy.env stop && \
	sudo docker-compose --env-file ./deploy.env rm -f

hard-restart:
	make hard-rm && \
	make up

soft-restart:
	make soft-rm && \
	make up