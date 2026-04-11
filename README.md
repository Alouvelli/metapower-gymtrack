# METAPOWER GymTrack

Système de gestion centralisé multisite pour le réseau de salles de sport METAPOWER.

## Périmètre

- **Multisite** : 2 salles, QR code universel valable partout
- **Membres** : inscription, génération QR, photos, plans d'abonnement
- **Mensualités** : suivi des paiements (Espèces, Wave, Orange Money, Carte)
- **Tickets d'entrée** : vente à la séance pour non-membres avec reçu PDF imprimable
- **Pointage** : scan QR à l'accueil (membres + tickets), validation en temps réel
- **Dashboard** : statistiques par site et consolidées
- **Rapports** : exports PDF / Excel
- **Notifications** : rappels email d'expiration de mensualité

## Stack

| Couche | Techno |
|--------|--------|
| Backend | Spring Boot 3.4 + Java 17 + Spring Security (JWT) + Spring Data JPA + Flyway + PostgreSQL 15 |
| QR Code | ZXing |
| Rapports | Apache POI (Excel), OpenPDF (PDF) |
| Email | Spring Mail + Thymeleaf |
| Frontend | Angular 15+ + Angular Material + Chart.js + @zxing/ngx-scanner |
| Infra | Docker Compose, Nginx |

## Structure

```
metapower-gymtrack/
├── backend/                # Spring Boot 3.x (Java 17)
│   └── src/main/java/sn/metapower/gymtrack/
│       ├── config/         # SecurityConfig, CorsConfig, OpenApi
│       ├── security/       # SecurityController, AccessKeyFilter
│       ├── entities/       # JPA entities
│       ├── repositories/   # Spring Data JPA
│       ├── dto/
│       ├── services/
│       ├── controllers/
│       └── exceptions/
├── frontend/               # Angular 15+
├── docker-compose.yml
└── docs/
```

## Démarrage local

```bash
docker compose up -d db
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && ng serve
```

Backend : http://localhost:8080
Frontend : http://localhost:4200
Swagger : http://localhost:8080/swagger-ui.html

## Phases de livraison

1. **Cadrage & Setup** (sem. 1-2)
2. **Modules Métier** (sem. 3-5) : membres, plans, mensualités, tarifs/tickets
3. **Pointage & Multisite** (sem. 6-7)
4. **Dashboard & Rapports** (sem. 8-9)
5. **Recette & Déploiement** (sem. 10-12)
