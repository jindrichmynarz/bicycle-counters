# Bicycle counters

This application collects Prague bicycle counters data for analyses by [Auto-mat](https://auto-mat.cz).

## Usage

Data from Prague bicycle counters is provided via the [Golemio platform](https://golemio.cz/cs/node/22), which exposes it via an API and [Microsoft Power BI dashboards](https://golemio.cz/insights/cyklodoprava). You can browse the [API documentation via Apiary](https://golemioapi.docs.apiary.io/#reference/traffic/bicycle-counters). The data is available in the [GeoJSON](https://tools.ietf.org/html/rfc7946) format.

To access the data via the Golemio API, [register](https://api.golemio.cz/api-keys/auth/sign-in), and generate an [API key](https://api.golemio.cz/api-keys/dashboard) to use as the `x-access-token` HTTP header in API requests.

You can use the [Apiary console](https://golemioapi.docs.apiary.io/#reference/traffic/bicycle-counters/get-all-bicycle-counters?console=1) to test API requests.

## License

Copyright © 2020-2022 Jindřich Mynarz

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.
