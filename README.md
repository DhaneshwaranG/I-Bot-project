# I-Bot — Intelligent Invoice Processing System

> Production-ready full-stack application for automated invoice processing using OCR, JWT authentication, and a modern React UI.

---

## ✨ Features

| Feature | Details |
|---|---|
| **OCR Processing** | Tesseract OCR extracts invoice number, vendor, date, GST, totals from PDF/PNG/JPG |
| **Smart Validation** | Per-field confidence scores, duplicate detection, human review workflow |
| **JWT Auth** | Secure login/register, auto token refresh, role-based access (ADMIN/USER) |
| **Invoice CRUD** | Create, read, update, delete, approve, reject, validate invoices |
| **Dashboard** | Monthly charts, top vendors, OCR accuracy, live counters |
| **Export** | Excel (.xlsx) bulk export, per-invoice PDF download |
| **Audit Logs** | Every action logged with user, timestamp, old/new value |
| **Vendor Management** | Vendor registry with GST/PAN linkage |
| **Duplicate Detection** | Automatic flagging of duplicate invoice numbers |

---

## 🗂️ Project Structure

```
ibot/
├── backend/                 # Spring Boot 3 + Java 17
│   ├── src/main/java/com/ibot/
│   │   ├── config/          # Security, CORS
│   │   ├── controller/      # REST endpoints
│   │   ├── dto/             # Request/Response DTOs
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Global error handling
│   │   ├── repository/      # Spring Data JPA
│   │   ├── security/        # JWT filter, UserDetails
│   │   └── service/         # Business logic, OCR, Reports
│   └── src/main/resources/
│       └── application.yml
├── frontend/                # React 18 + Vite + Tailwind
│   └── src/
│       ├── components/      # Layout, ProtectedRoute
│       ├── context/         # AuthContext
│       ├── pages/           # Login, Dashboard, Upload, Validate, History, Profile
│       ├── services/        # Axios API calls
│       └── utils/           # Formatters, StatusChip
├── database/
│   ├── schema.sql           # Full MySQL DDL
│   └── seed.sql             # Sample data + demo user
├── docker-compose.yml       # One-command startup
└── .env.example
```

---

## 🚀 Quick Start

### Option A — Docker (recommended)

```bash
git clone <repo>
cd ibot
docker-compose up --build
```

Open **http://localhost** — done!

### Option B — Local development

#### Prerequisites
- Java 17+, Maven 3.8+
- Node 20+, npm
- MySQL 8.0
- Tesseract OCR (`sudo apt install tesseract-ocr` / `brew install tesseract`)

#### 1. Database
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p ibot_db < database/seed.sql
```

#### 2. Backend
```bash
cd backend
cp ../.env.example .env   # edit DB credentials
mvn spring-boot:run
# Starts on http://localhost:8080
```

#### 3. Frontend
```bash
cd frontend
npm install
cp ../.env.example .env   # set VITE_API_URL=http://localhost:8080/api
npm run dev
# Opens http://localhost:5173
```

---

## 🔑 Demo Credentials

| Role | Email | Password |
|---|---|---|
| Admin | admin@ibot.com | password123 |
| User | priya@ibot.com | password123 |

---

## 📡 API Reference

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create account |
| `POST` | `/api/auth/login` | Login, get JWT |
| `POST` | `/api/auth/refresh` | Refresh access token |

### Invoices

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/invoices/upload` | Upload PDF/image, run OCR |
| `POST` | `/api/invoices` | Create invoice manually |
| `GET` | `/api/invoices` | Search + paginate all invoices |
| `GET` | `/api/invoices/my` | Current user's invoices |
| `GET` | `/api/invoices/{id}` | Get invoice by ID |
| `PUT` | `/api/invoices/{id}` | Update invoice |
| `POST` | `/api/invoices/{id}/validate` | Save corrected OCR data |
| `POST` | `/api/invoices/{id}/approve` | Approve invoice |
| `POST` | `/api/invoices/{id}/reject` | Reject with reason |
| `DELETE` | `/api/invoices/{id}` | Delete invoice |
| `GET` | `/api/invoices/export/excel` | Download XLSX report |
| `GET` | `/api/invoices/{id}/pdf` | Download invoice PDF |

### Dashboard

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/dashboard/stats` | Full stats, charts, vendor breakdown |

### Query params for `GET /api/invoices`
```
?status=PENDING          # Filter by status
&vendorName=Acme         # Search vendor
&startDate=2024-01-01    # Date range
&endDate=2024-12-31
&minAmount=1000          # Amount range
&maxAmount=50000
&page=0&size=20          # Pagination
&sortBy=createdAt&sortDir=desc
```

---

## 🗄️ Database Schema

```
users ──────────────────────────────────────────┐
  id, name, email, password, role, department   │
                                                 │
vendors                                          │
  id, name, vendor_code, gst_number, pan_number │
       │                                         │
invoices ◄──────────────────────────────────────┘
  id, invoice_number, vendor_name, status
  subtotal, gst_amount, gst_rate, total_amount
  ocr_confidence, is_duplicate, file_path
       │
invoice_line_items
  id, description, quantity, unit_price, gst_rate

audit_logs
  entity_type, entity_id, action, old/new_value, user_id
```

---

## 🧩 Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | React 18, Vite, React Router 6, TanStack Query, Recharts, Tailwind CSS, Axios |
| **Backend** | Spring Boot 3.2, Spring Security 6, Spring Data JPA |
| **Auth** | JWT (jjwt 0.11.5), BCrypt |
| **OCR** | Tesseract (Tess4J 5.8) |
| **PDF** | Apache PDFBox 3, iText 5 |
| **Excel** | Apache POI 5 |
| **Database** | MySQL 8.0, Hibernate |
| **Deployment** | Docker, Docker Compose, Nginx |

---

## ⚙️ Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `ibot_db` | Database name |
| `DB_USERNAME` | `root` | DB username |
| `DB_PASSWORD` | — | DB password |
| `JWT_SECRET` | — | **Change in production** |
| `JWT_EXPIRATION` | `86400000` | Access token TTL (ms) |
| `UPLOAD_DIR` | `./uploads` | Invoice file storage |
| `TESSDATA_PATH` | `/usr/share/tessdata` | Tesseract data directory |
| `VITE_API_URL` | `/api` | Frontend API base URL |

---

## 📦 Production Deployment

### Build artifacts
```bash
# Backend JAR
cd backend && mvn clean package -DskipTests
# Creates: target/ibot-backend-1.0.0.jar

# Frontend dist
cd frontend && npm run build
# Creates: dist/ folder for Nginx
```

### AWS / Render / Railway
1. Push to GitHub
2. Set environment variables in your platform
3. Backend: `java -jar ibot-backend-1.0.0.jar`
4. Frontend: serve `dist/` as static site

---

## 🔒 Security Notes

- BCrypt password hashing with cost factor 12
- JWT HS256 with configurable secret — **use a 256-bit+ random value in production**
- CORS locked to `FRONTEND_URL` env variable
- File upload limited to 20 MB, PDF/PNG/JPG only
- All endpoints require valid JWT except `/api/auth/**`
- Admin-only routes protected by `@PreAuthorize("hasRole('ADMIN')")`
