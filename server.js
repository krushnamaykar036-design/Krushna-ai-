const express = require("express");

const app = express();
app.use(express.json());

app.get("/", (req, res) => {
  res.send("Krushna AI Server चालू आहे!");
});

app.post("/chat", (req, res) => {
  const message = req.body.message || "";

  res.json({
    reply: "Krushna AI Server ला message मिळाला: " + message
  });
});

app.listen(process.env.PORT || 3000, () => {
  console.log("Krushna AI Server started");
});
