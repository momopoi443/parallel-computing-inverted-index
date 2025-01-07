docker run --name inverted-index --mount src=C:\Users\alex\Desktop\docs,target=/app/data,type=bind -p 127.0.0.1:8080:8080/tcp --env-file .env parallel-computing-inverted-index
