FROM python:3.11-alpine
RUN pip install --no-cache-dir aiohttp==3.10.5
WORKDIR /app
COPY app.py /app/app.py
VOLUME ["/data/img-cache"]
EXPOSE 8088
CMD ["python", "-u", "app.py"]
