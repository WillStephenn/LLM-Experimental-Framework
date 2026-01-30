---
description: 
---

Run the following frontend checks and resolve any issues it produces:

```
cd frontend
```
```
npm ci && npm run lint && npm run format:check && npx tsc --noEmit && npm run test && npm run build
```