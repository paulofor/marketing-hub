toda tela de edição/inserção de dados por formulário deve ter esse tipo de log:
onClick={handleSubmit(onSubmit, (errors) => {
console.log('Validation errors', errors);
})}

além disso, todo campo que aciona serviços do Worker IA deve possuir um tooltip explicando seu funcionamento.
