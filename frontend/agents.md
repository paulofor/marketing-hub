toda tela de edição/inserção de dados por formulário deve ter esse tipo de log:
            onClick={handleSubmit(onSubmit, (errors) => {
              console.log('Validation errors', errors);
            })}