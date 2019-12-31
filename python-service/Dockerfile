FROM python:3.7.3-alpine3.9
MAINTAINER Marcus Barcelos "mvlbarcelos@gmail.com"
COPY . /app
WORKDIR /app
RUN pip install --no-cache-dir -r requirements.txt
ENTRYPOINT ["python"]
CMD ["app.py"]
