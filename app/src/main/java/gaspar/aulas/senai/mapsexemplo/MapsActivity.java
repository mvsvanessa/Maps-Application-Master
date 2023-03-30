package gaspar.aulas.senai.mapsexemplo;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //O classe abaixo irá fornecer os métodos para interagir com o GPS bem como recuperar os dados do posicionamento
    private FusedLocationProviderClient servicoLocalizacao;

    //Já vem com o código original, serve para referenciar o Mapa que será montado na tela bem
    //como usar os métodos para posicionar, adicionar marcador e tudo mais
    private GoogleMap mMap;

    //Variável para armazenar se o usuario clicou em permitir ou não
    private boolean permitiuGPS = false;

    //Variável para armazenar o ponto retornoado pelo GPS
    Location ultimaPosicao;

    //Objetos para os componentes da tela
    private EditText campoLocal;
    private TextView tvDuracao, tvDistancia;

    //Lista para armazenar todos os pontos de Latitude e Longitude de uma rota
    List<LatLng> pontosRota;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Inicializar o fragmento aonde o mapa está localizado dentro da Activity [código original]
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Inicialização dos componentes da tela
        Button btBuscar = findViewById(R.id.btBuscar);
        campoLocal = findViewById(R.id.campoLocal);
        tvDistancia = findViewById(R.id.tvDistancia);
        tvDuracao = findViewById(R.id.tvDuracao);

        //Chama o serviço de localização do Andrdoid e atribui ao nosso objeto
        servicoLocalizacao = LocationServices.getFusedLocationProviderClient(this);

        //Verificar se o  usuário já deu permissão para o uso do GPS
        //No caso do GPS, quando o usuário clicar para permitir ou não o acesso aos dados de localização,
        //será executado o método onRequestPermissionsResults. Dentro desse método já podemos pegar
        //os dados de latitude e longitude.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 120);
        } else {
            permitiuGPS = true;
        }

        //Recuperação do gerenciador de localização
        LocationManager gpsHabilitado = (LocationManager) getSystemService(LOCATION_SERVICE);
        //Verificação se o GPS está habilitado, caso não esteja...
        if (!gpsHabilitado.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //... abre a tela de configurações na opção para habilitar o GPS ou não
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            Toast.makeText(getApplicationContext(), "Para este aplicativo é necessário habilitar o GPS", Toast.LENGTH_LONG).show();
        }

        //Evento para o botão de buscar uma localização
        btBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ultimaPosicao != null){
                    buscaCoordenadas("https://maps.googleapis.com/maps/api/directions/json?origin=" + ultimaPosicao.getLatitude() + "," + ultimaPosicao.getLongitude() +"&destination=" + campoLocal.getText().toString() + "&key=AIzaSyBoDavGlqUm9rLrj5Rb2TC9sTdzbTXU9TY");
                }
            }
        });
    }

    //Quando o mapa estiver pronto e carregado na tela, irá chamar o método abaixo
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //[Código original]
        //Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));

        adicionaComponentesVisuais();
        recuperarPosicaoAtual();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.684, 133.903), 4));

    }

    //Adiciona o botão para centralizar o mapa na posição atual. Esse botão é aquele parecido com um
    //alvo que fica no canto superior direito do mapa.
    private void adicionaComponentesVisuais() {
        //Se o objeto do mapa não existir, encerra o carregamento no return
        if (mMap == null) {
            return;
        }
        //Try/catch somente para não aparecer algum erro ao usuário com relação à permissão
        try {
            //Teste para verificar se o usuário já permitiu o acesso ao GPS, caso sim...
            if (permitiuGPS) {
                //Adiciona o botão que quando clicado vai para a posição atual do celular/GPS
                mMap.setMyLocationEnabled(true);
                //Habilita o botão para ser clicado
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else { //Se caso o usuário não permitiu o acesso aos dados de localização
                mMap.setMyLocationEnabled(false); //Remove o botão
                mMap.getUiSettings().setMyLocationButtonEnabled(false); //Desabilita o botão

                //Limpa a última posição recuperada pois não é possível acessar o GPS sem a permissão
                ultimaPosicao = null;

                //Pede a permissão novamente
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 120);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    //Método que irá receber todas as atualizações enviadas pelo GPS, isto é, se mudar algum
    //ponto de latitude ou longitude o GPS irá informar o celular e o método getLastLocation()
    //irá recuperar esse valor
    private void recuperarPosicaoAtual() {
        try {
            //Testa se a pessoa permitiu o uso dos dados de localização
            if (permitiuGPS) {
                Task locationResult = servicoLocalizacao.getLastLocation();
                //Assim que os dados estiverem recuperados
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(Task task) {
                        if (task.isSuccessful()) {
                            //Recupera os dados de localização da última posição
                            ultimaPosicao = (Location) task.getResult();

                            //Se for um valor válido
                            if (ultimaPosicao != null) {
                                //Move a câmera para o ponto recuperado e aplica um Zoom de 15 (valor padrão)
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(ultimaPosicao.getLatitude(),
                                                ultimaPosicao.getLongitude()), 15));
                            }
                        } else {
                            //Exibe um Toast se o valor que recuperou do GPS não é válido
                            Toast.makeText(getApplicationContext(), "Não foi possível recuperar a posição.", Toast.LENGTH_LONG).show();
                            //Escreve o erro no LogCat
                            Log.e("TESTE_GPS", "Exception: %s", task.getException());
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void buscaCoordenadas(String url){
        //Chamada da classe Volley para requisições à API do Google Routes
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        //Configura o método e envio e implementa o listener
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            //Quando houver uma requisição e resposta válida...
            @Override
            public void onResponse(String response) {
                //Criação de vetores JSON para armazenar o conteúdo de cada seção do retorno
                JSONArray listaRotas = null; //Routes
                JSONArray listaTrechos = null; //Legs
                JSONArray listaCoordenadas = null; //Steps

                //Inicilaiza a lista aonde ficarão armazenadas as coordenadas de latitude e longitude
                pontosRota = new ArrayList<>();
                //Limpa a lista dos pontos para uma nova consulta
                pontosRota.clear();
                //Limpa o caminho no mapa, para consultar um novo trecho e montar os pontos
                mMap.clear();

                try {
                    //Converte a resposta da chamada da API para um objeto JSON
                    JSONObject lista = new JSONObject(response);
                    //Retira do objeto "lista" somente o trecho marcado como "routes"
                    listaRotas = lista.getJSONArray("routes");

                    //Laço for para percorrer todas as posições/trechos dentro das rotas
                    for(int i=0 ; i<listaRotas.length() ; i++) {
                        //Dentro de cada rota, recupera cada trecho (legs) da viagem
                        listaTrechos = ((JSONObject) listaRotas.get(i)).getJSONArray("legs");

                        //Passando por cada trecho de cada rota encontrada
                        for (int j=0; j<listaTrechos.length() ; j++) {
                            //Recupera o dado da duração e distância do trecho inteiro que está dentro do objeto "duration" e no campo "text"
                            String duracao = ((JSONObject) listaTrechos.get(j)).getJSONObject("duration").getString("text");
                            String distancia = ((JSONObject) listaTrechos.get(j)).getJSONObject("distance").getString("text");

                            //Atribui os valores para os TextViews
                            tvDuracao.setText("Duração: " + duracao);
                            tvDistancia.setText("Distância: " + distancia);

                            //Dentro de cada trecho percorrido, passa por todos os pontos/passos (steps) de orientação
                            listaCoordenadas = ((JSONObject) listaTrechos.get(j)).getJSONArray("steps");

                            //Percorrendo cada ponto do nosso caminho
                            for (int k=0 ; k<listaCoordenadas.length() ; k++) {
                                //Variável a ser utilizada para armazenar os pontos do caminho retornados pela API
                                String polyline = (String) ((JSONObject) ((JSONObject) listaCoordenadas.get(k)).get("polyline")).get("points");

                                //Criando uma lista para armazenar os pontos já decodificados
                                List list = decodePoly(polyline);

                                //Passando por cada ponto da lista
                                for (int l = 0; l < list.size(); l++) {
                                    //Log.d("GPS_ROTAS", "LAT " + Double.toString(((LatLng) list.get(l)).latitude));
                                    //Log.d("GPS_ROTAS", "LONG " + Double.toString(((LatLng) list.get(l)).longitude));

                                    //Recuperando cada ponto de latitude e longitude da lista
                                    LatLng ponto = new LatLng(((LatLng) list.get(l)).latitude, ((LatLng) list.get(l)).longitude);
                                    //Armazenando cada ponto em uma outra List do tipo LatLng
                                    pontosRota.add(ponto);
                                }
                            }
                        }
                    }

                    //Após encontrar todos os pontos dentro da rota, trechos e passos a serem percorridos
                    //adicionamos essas informações para criar uma linha ligando todos os pontos
                    mMap.addPolyline(new PolylineOptions().addAll(pontosRota));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError erro) {
                Log.d("GPS_ROTAS", erro.toString());

            }
        });

        //Executa o comando
        queue.add(stringRequest);
    }

    /**
     * Método para decodificar as informações dos pontos enviados pela API
     * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     * */
    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

}
