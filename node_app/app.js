const express = require('express');
const stripe = require('stripe')("sk_test_51JCRzOSADReF0qa1JJ3wqe5yZJf96qXuGzLui0jRpbL9jSAdFll2a1jEoE8w1dWDW0qAVcMnHB6PFK2KWULHiLmZ00IYGkUU7S");
const bodyParser = require('body-parser');

const app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false}));


app.get('/', (req, res) => {
    res.send("Start page .. Nothing to show.");    
});

app.post('/charge', (req, res) => {
    const amount = 2500;
    
});

app.use(express.static(`${__dirname}/public`));

const port = process.env.PORT || 5000;

app.listen(port, () => {
    console.log(`Server started on port ${port}`);
});