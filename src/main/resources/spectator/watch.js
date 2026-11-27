'use strict';
const el = id => document.getElementById(id);
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const post = body => ({method: 'POST', headers: {'X-VarZinho': 'spectator'}, body});
async function request(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) throw new Error(await response.text());
    return response;
}

async function watch() {
    const video = el('player');
    const notice = el('liveStatus');
    const buy = el('buy');
    const Engine = window.MediaSource || window.ManagedMediaSource;
    const mime = 'video/mp4; codecs="avc1.42c01f"';
    if (!Engine || !Engine.isTypeSupported(mime)) {
        notice.textContent = 'Este navegador não reproduz a transmissão. Tente um navegador com suporte a vídeo H.264 por MediaSource.';
        return;
    }
    let match;
    for (let attempt = 0; attempt < 15; attempt++) {
        match = await fetch('/match');
        if (match.ok) break;
        notice.textContent = 'Aguardando as primeiras imagens da quadra…';
        await sleep(1000);
    }
    if (!match.ok) throw new Error('A câmera ainda não está transmitindo.');
    const [session, newest, court, sales, duration] = (await match.text()).split(' ');
    const segmentMs = Number(duration);
    const canBuy = sales === 'buy' || sales === 'demo';
    if (sales === 'demo') buy.textContent = 'Simular compra · R$ 5,00 (sem cobrança)';
    if (!canBuy) buy.textContent = 'Compras indisponíveis';
    el('court').textContent = 'Quadra ' + court + ' · Transmissão ao vivo';
    const base = '/live/' + session + '/';
    const source = new Engine();
    const opened = new Promise(resolve => source.addEventListener('sourceopen', resolve, {once:true}));
    video.src = URL.createObjectURL(source);
    await opened;
    const buffer = source.addSourceBuffer(mime);
    function update(action) {
        return new Promise((resolve, reject) => {
            function done() {
                buffer.removeEventListener('error', failed);
                resolve();
            }
            function failed() {
                buffer.removeEventListener('updateend', done);
                reject(new Error('Falha ao reproduzir o vídeo.'));
            }
            buffer.addEventListener('updateend', done, {once:true});
            buffer.addEventListener('error', failed, {once:true});
            action();
        });
    }
    async function append(name) {
        const bytes = await (await request(base + name)).arrayBuffer();
        await update(() => buffer.appendBuffer(bytes));
    }
    const first = Number(newest);
    await append('init.mp4');
    await append('part-' + first + '.m4s');
    const origin = buffer.buffered.start(0);
    let following = true;
    let automaticSeek = origin;
    video.currentTime = origin;
    function seekLive() {
        if (!buffer.buffered.length) return;
        const last = buffer.buffered.length - 1;
        automaticSeek = Math.max(buffer.buffered.start(last), buffer.buffered.end(last) - 0.8);
        video.currentTime = automaticSeek;
    }
    video.addEventListener('pause', () => { following = false; });
    video.addEventListener('seeking', () => {
        if (automaticSeek === null || Math.abs(video.currentTime - automaticSeek) > 0.1) following = false;
    });
    video.addEventListener('seeked', () => { automaticSeek = null; });
    el('goLive').onclick = () => {
        following = true;
        seekLive();
        video.play().catch(() => { notice.textContent = 'Toque em reproduzir para assistir.'; });
    };
    video.play().catch(() => { notice.textContent = 'Toque em reproduzir para assistir.'; });
    const position = () => Math.round((video.currentTime - origin) * 1000) + first * segmentMs;
    let preparing = false;
    video.ontimeupdate = () => {
        const remaining = Math.max(0, Math.ceil((30000 - position()) / 1000));
        buy.disabled = !canBuy || preparing || remaining > 0;
        if (!preparing) notice.textContent = canBuy && remaining
                ? 'Mais ' + remaining + ' s de partida para reservar um lance.'
                : (following ? 'Ao vivo' : 'Reprodução') + ' · Quadra ' + court;
    };
    buy.onclick = async () => {
        if (preparing) return;
        const end = position();
        preparing = true;
        buy.disabled = true;
        notice.textContent = 'Preparando os 30 segundos do lance…';
        try {
            const secret = crypto.randomUUID();
            const result = await request('/reserve/' + session, post(secret + ' ' + end));
            location.replace('/#pedido=' + await result.text());
            location.reload();
        } catch (error) {
            notice.textContent = error.message;
            preparing = false;
            buy.disabled = position() < 30000;
        }
    };
    let last = first;
    for (;;) {
        const [current, latest] = (await (await request('/match')).text()).split(' ');
        if (current !== session) throw new Error('Começou uma nova transmissão. Recarregue a página.');
        if (following && !video.paused && Number(latest) - last > 4) last = Number(latest) - 1;
        while (last < Number(latest)) {
            await append('part-' + (++last) + '.m4s');
            if (following && !video.paused && buffer.buffered.length
                    && buffer.buffered.end(buffer.buffered.length - 1) - video.currentTime > 3) seekLive();
            if (buffer.buffered.length && video.currentTime - buffer.buffered.start(0) > 75)
                await update(() => buffer.remove(buffer.buffered.start(0), video.currentTime - 45));
            if (buffer.buffered.length && buffer.buffered.end(buffer.buffered.length - 1) - video.currentTime > 110)
                throw new Error('A reprodução ficou pausada por muito tempo. Recarregue para voltar ao vivo.');
        }
        await sleep(400);
    }
}

async function purchase(secret) {
    el('live').hidden = true;
    el('order').hidden = false;
    const prefix = '/purchase/' + secret + '/';
    const status = el('orderStatus');
    const form = el('checkout');
    const demo = el('demoPayment');
    async function check() {
        const response = await fetch(prefix + 'status');
        const simulated = response.headers.get('X-VarZinho-Sales') === 'demo';
        if (response.status === 402) {
            demo.hidden = !simulated;
            form.hidden = simulated || !el('payment').hidden;
            return false;
        }
        if (response.status === 409) {
            form.hidden = el('payment').hidden = true;
            status.textContent = await response.text();
            return true;
        }
        if (!response.ok) throw new Error(await response.text());
        demo.hidden = form.hidden = el('payment').hidden = true;
        el('ready').hidden = false;
        el('clip').src = el('download').href = prefix + 'clip.mp4';
        status.textContent = simulated ? 'Compra de teste concluída, sem cobrança. Seu lance está disponível.'
                : 'Pagamento confirmado. Seu lance está disponível.';
        return true;
    }
    el('confirmDemo').onclick = async event => {
        const button = event.currentTarget;
        button.disabled = true;
        try {
            await request(prefix + 'demo', post(''));
            await check();
        } catch (error) { status.textContent = error.message; }
        finally { button.disabled = false; }
    };
    async function showPayment(code) {
        el('code').value = code;
        form.hidden = true;
        el('payment').hidden = false;
        el('qr').onerror = () => { el('qr').hidden = true; };
        el('qr').src = prefix + 'qr.png';
        status.textContent = 'Pague o Pix no aplicativo do seu banco. A confirmação aparece aqui.';
        while (true) {
            await sleep(6000);
            try { if (await check()) return; }
            catch { status.textContent = 'A confirmação está demorando. Vamos consultar o pagamento novamente.'; }
        }
    }
    form.onsubmit = async event => {
        event.preventDefault();
        const button = form.querySelector('button');
        button.disabled = true;
        try {
            const payer = el('email').value.trim() + '\n' + el('cpf').value.replace(/[.\s-]/g, '');
            await showPayment(await (await request(prefix + 'pix', post(payer))).text());
        } catch (error) { status.textContent = error.message; form.hidden = false; }
        finally { button.disabled = false; }
    };
    el('copy').onclick = async () => {
        try { await navigator.clipboard.writeText(el('code').value); status.textContent = 'Código Pix copiado.'; }
        catch { el('code').select(); status.textContent = 'Selecione e copie o código acima.'; }
    };
    try {
        if (await check()) return;
        if (!demo.hidden) return;
        const code = await (await request(prefix + 'pix')).text();
        if (code) await showPayment(code);
    } catch (error) { status.textContent = error.message; }
}
const order = location.hash.match(/^#pedido=([a-f0-9-]{36})$/);
if (order) purchase(order[1]);
else watch().catch(error => {
    el('buy').disabled = true;
    el('player').ontimeupdate = null;
    el('liveStatus').textContent = error.message + ' Recarregue a página para reconectar.';
});
