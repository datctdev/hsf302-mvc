# Ollama local (Docker) – model nhẹ cho AI recommend

## 1. Chạy Ollama trên Docker

```bash
# Từ thư mục gốc dự án
docker compose up -d ollama
```

Ollama lắng nghe tại **http://localhost:11434** (đổi port bằng biến `OLLAMA_PORT` trong `.env` nếu cần).

Kiểm tra service:

```bash
curl http://localhost:11434/api/tags
```

## 2. Model nhẹ phù hợp cho chức năng AI recommend

| Model | Kích thước (pull) | RAM ước lượng | Dùng cho |
|-------|--------------------|---------------|----------|
| **nomic-embed-text** | ~274 MB | ~500 MB | **Embedding** – tìm sản phẩm tương tự (semantic), RAG. Không chat. |
| **qwen2.5:0.5b** | ~377 MB | ~1 GB | Chat/complete rất nhẹ, có thể dùng gợi ý ngắn. |
| **tinyllama** | ~638 MB | ~1–2 GB | Chat nhỏ, tiếng Anh tốt hơn tiếng Việt. |
| **llama3.2:1b** | ~1.3 GB | ~2 GB | Chat 1B của Meta, cân bằng. |
| **phi3:mini** | ~2.3 GB | ~4 GB | Chat 3.8B, chất lượng tốt, vẫn chạy được trên máy 8GB RAM. |
| **gemma2:2b** | ~1.6 GB | ~3 GB | Chat 2B của Google. |

**Gợi ý cho “AI recommend” trên localhost:**

- **Chỉ cần “sản phẩm tương tự” theo nội dung (tên/mô tả):** dùng **nomic-embed-text** (embedding) → encode sản phẩm + query → so sánh vector. Nhẹ nhất, phù hợp local.
- **Cần thêm câu chữ gợi ý / chatbot:** thêm một model chat nhỏ, ví dụ **qwen2.5:0.5b** hoặc **llama3.2:1b**.

## 3. Pull và chạy thử từng model

Trong container:

```bash
# List model đã có
docker exec -it app-ollama ollama list

# Pull từng model (chọn 1 hoặc vài cái để test)
docker exec -it app-ollama ollama pull nomic-embed-text
docker exec -it app-ollama ollama pull qwen2.5:0.5b
docker exec -it app-ollama ollama pull llama3.2:1b
docker exec -it app-ollama ollama pull tinyllama
docker exec -it app-ollama ollama pull phi3:mini
```

Chat nhanh trong terminal:

```bash
docker exec -it app-ollama ollama run qwen2.5:0.5b "Gợi ý 3 từ khóa tìm kiếm cho người thích laptop gaming."
```

## 4. Kiểm tra qua API (localhost)

### 4.1 Chat (generate)

```bash
curl http://localhost:11434/api/generate -d "{
  \"model\": \"qwen2.5:0.5b\",
  \"prompt\": \"User vừa tìm: laptop. Gợi ý ngắn 1 câu cho section Gợi ý cho bạn.\",
  \"stream\": false
}"
```

### 4.2 Embedding (cho “sản phẩm tương tự”)

```bash
curl http://localhost:11434/api/embeddings -d "{
  \"model\": \"nomic-embed-text\",
  \"prompt\": \"Laptop gaming ASUS ROG 15 inch\"
}"
```

Trả về mảng vector; so sánh với embedding của các sản phẩm (cosine similarity) để rank “tương tự”.

### 4.3 Danh sách model đã cài

```bash
curl http://localhost:11434/api/tags
```

## 5. Gợi ý tích hợp vào dự án

- **Recommend “theo nội dung” (giống Shopee “xem thêm”):**  
  Dùng **nomic-embed-text**:  
  - Khi crawl/index sản phẩm: gọi `POST /api/embeddings` với `name + description` → lưu vector (DB hoặc file).  
  - Khi user xem sản phẩm hoặc search: embed query/sản phẩm hiện tại → tìm top-K gần nhất → trả về danh sách gợi ý.

- **Recommend “câu chữ” (chatbot, caption block “Gợi ý cho bạn”):**  
  Dùng **qwen2.5:0.5b** hoặc **llama3.2:1b**:  
  - Gọi `POST /api/generate` với prompt kiểu: “Dựa trên từ khóa: [keyword], viết 1 câu ngắn gợi ý mua sắm.” → hiển thị trên UI.

- **Tài nguyên:**  
  Chạy đồng thời **nomic-embed-text** + **qwen2.5:0.5b** trên một máy 4–8 GB RAM thường ổn; nếu RAM thấp thì chỉ dùng **nomic-embed-text** trước.

## 6. Lưu ý

- **CPU only:** Ảnh `ollama/ollama` mặc định chạy được trên CPU; không cần GPU để test.
- **GPU:** Nếu có NVIDIA GPU và đã cài [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/), có thể bật block `deploy.resources.reservations.devices` trong `docker-compose.yml` cho service `ollama` để chạy nhanh hơn.
- **Port:** Mặc định `11434`. Đổi bằng biến môi trường `OLLAMA_PORT` (ví dụ trong `.env`) và map lại trong `docker-compose`.
