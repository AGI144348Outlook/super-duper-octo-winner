const { Pool } = require("pg");

let pool;
function getPool() {
  if (!process.env.DATABASE_URL) throw new Error("relay_storage_not_configured");
  if (!pool) {
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      max: 2,
      idleTimeoutMillis: 5000,
      connectionTimeoutMillis: 8000,
      application_name: "nexusportal-relay"
    });
  }
  return pool;
}
module.exports = { query: (...args) => getPool().query(...args) };
